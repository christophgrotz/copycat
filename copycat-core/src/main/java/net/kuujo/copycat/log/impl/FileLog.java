/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kuujo.copycat.log.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import net.kuujo.copycat.log.Entry;
import net.kuujo.copycat.log.Log;
import net.kuujo.copycat.log.LogException;
import net.kuujo.copycat.serializer.Serializer;
import net.kuujo.copycat.serializer.SerializerFactory;

/**
 * File-based log implementation.<p>
 *
 * This log entries to a {@link RandomAccessFile}. Pointers are
 * managed internally by the <code>FileLog</code>.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class FileLog implements Log {
  private static final Serializer serializer  = SerializerFactory.getSerializer();
  private static final String separator = System.getProperty("line.separator");
  private File f;
  private RandomAccessFile file;
  private long firstIndex;
  private long lastIndex;

  public FileLog(String fileName) {
    this.f = new File(fileName);
  }

  public FileLog(File file) {
    this.f = file;
  }

  @Override
  public synchronized void open() {
    if (!f.exists()) {
      try {
        File parent = f.getParentFile();
        if (parent != null) {
          parent.mkdirs();
        }
        f.createNewFile();
      } catch (IOException e) {
        throw new LogException(e);
      }
    }

    try {
      file = new RandomAccessFile(f.getAbsolutePath(), "rw");
    } catch (FileNotFoundException e) {
      throw new LogException(e);
    }

    String line = null;
    try {
      String firstLine = file.readLine();
      if (firstLine != null) {
        firstIndex = parseIndex(firstLine);
      }
      String lastLine = firstLine;
      while ((line = file.readLine()) != null) {
        lastLine = line;
      }

      if (lastLine != null) {
        lastIndex = parseIndex(lastLine);
      }
    } catch (IOException e) {
      throw new LogException(e);
    }
  }

  private long parseIndex(String line) {
    return Long.valueOf(line.substring(0, line.indexOf(":")));
  }

  @Override
  public long size() {
    return lastIndex - firstIndex;
  }

  @Override
  public boolean isEmpty() {
    return firstIndex == 0;
  }

  @Override
  public synchronized long appendEntry(Entry entry) {
    long index = lastIndex+1;
    try {
      String bytes = new StringBuilder()
        .append(index)
        .append(':')
        .append(new String(serializer.writeValue(entry)))
        .append(separator)
        .toString();
      file.writeBytes(bytes);
      lastIndex++;
      if (firstIndex == 0) {
        firstIndex = 1;
      }
      return index;
    } catch (IOException e) {
      throw new LogException(e);
    }
  }

  @Override
  public boolean containsEntry(long index) {
    return indexInRange(index);
  }

  @Override
  public synchronized Entry getEntry(long index) {
    Entry entry = null;
    if (indexInRange(index)) {
      try {
        findFilePointer(index);
        String line = file.readLine();
        if (line != null) {
          entry = serializer.readValue(line.substring(String.valueOf(index).length() + 1).getBytes(), Entry.class);
        }
        file.seek(file.length());
      } catch (IOException e) {
        throw new LogException(e);
      }
    }
    return entry;
  }

  @Override
  public synchronized Log setEntry(long index, Entry entry) {
    try {
      long pointer = findFilePointer(index);
      String line = file.readLine();
      int length = line.length();
      String bytes = new StringBuilder()
        .append(index)
        .append(':')
        .append(new String(serializer.writeValue(entry)))
        .append(separator)
        .toString();
      int newLength = bytes.length();
      if (newLength > length) {
        expandFile(pointer+length, pointer+newLength);
      } else if (length > newLength) {
        compactFile(pointer+newLength, pointer+length);
      }
      file.seek(pointer);
      file.writeBytes(bytes);
      file.seek(file.length());
    } catch (IOException e) {
      throw new LogException(e);
    }
    return this;
  }

  @Override
  public long firstIndex() {
    return firstIndex;
  }

  @Override
  public Entry firstEntry() {
    return getEntry(firstIndex);
  }

  @Override
  public long lastIndex() {
    return lastIndex;
  }

  @Override
  public Entry lastEntry() {
    return getEntry(lastIndex);
  }

  @Override
  public List<Entry> getEntries(long start, long end) {
    List<Entry> entries = new ArrayList<>();
    for (long i = start; i <= end; i++) {
      Entry entry = getEntry(i);
      if (entry != null) {
        entries.add(entry);
      }
    }
    return entries;
  }

  @Override
  public synchronized void removeBefore(long index) {
    if (indexInRange(index)) {
      long pointer = findFilePointer(index);
      compactFile(pointer, 0);
      firstIndex = index;
    }
  }

  @Override
  public synchronized void removeAfter(long index) {
    if (firstIndex > 0 && indexInRange(index)) {
      try {
        long pointer = findFilePointer(index+1);
        file.setLength(pointer);
        lastIndex = index;
        file.seek(file.length());
      } catch (IOException e) {
        throw new LogException(e);
      }
    }
  }

  /**
   * Returns a boolean indicating whether the given index is within the range
   * of the log.
   */
  private boolean indexInRange(long index) {
    return index >= firstIndex && index <= lastIndex;
  }

  /**
   * Finds the file pointer for the entry at the given index.
   */
  private long findFilePointer(long index) {
    if (!indexInRange(index)) {
      throw new IndexOutOfBoundsException("Index out of bounds");
    }
    try {
      file.seek(0);
      long currentIndex = firstIndex;
      while (currentIndex < index) {
        file.readLine();
        currentIndex++;
      }
      return file.getFilePointer();
    } catch (IOException e) {
      throw new LogException(e);
    }
  }

  /**
   * Compacts the file by rewriting later bytes over earlier bytes.
   */
  private void compactFile(long from, long to) {
    if (to > from) throw new IllegalArgumentException("Cannont compact file from " + from + " to " + to);
    try {
      long originalLength = file.length();
      long readCursor = from;
      long writeCursor = to;
      int bufferSize = 4096;
      while (readCursor < originalLength) {
        int bytesToRead = (int) (readCursor + bufferSize > originalLength ? originalLength - readCursor : bufferSize);
        file.seek(readCursor);
        byte[] bytes = new byte[bytesToRead];
        file.read(bytes, 0, bytesToRead);
        readCursor += bytesToRead;
        file.seek(writeCursor);
        file.write(bytes);
        writeCursor += bytesToRead;
      }
      file.setLength(originalLength - (from - to));
      file.seek(file.length());
    } catch (IOException e) {
      throw new LogException(e);
    }
  }

  /**
   * Extends the length of the file by moving bytes from the start position to the end position.
   */
  private void expandFile(long from, long to) {
    if (from > to) throw new IllegalArgumentException("Cannot expand file from " + from + " to " + to);
    try {
      long difference = to - from;
      long originalLength = file.length();
      long newLength = originalLength + difference;
      file.setLength(newLength);
      long readCursor = originalLength;
      long writeCursor = newLength;
      int bufferSize = 4096;
      while (readCursor > from) {
        int bytesToRead = (int) (readCursor - bufferSize < from ? readCursor - from : bufferSize);
        readCursor -= bytesToRead;
        file.seek(readCursor);
        byte[] bytes = new byte[bytesToRead];
        file.read(bytes, 0, bytesToRead);
        writeCursor -= bytesToRead;
        file.seek(writeCursor);
        file.write(bytes);
      }
      file.seek(file.length());
    } catch (IOException e) {
      throw new LogException(e);
    }
  }

  @Override
  public void close() {
    try {
      if (file != null) {
        file.close();
      }
    } catch (IOException e) {
      throw new LogException(e);
    }
  }

  @Override
  public void delete() {
    try {
      file.close();
    } catch (IOException e) {
      throw new LogException(e);
    } finally {
      if (f != null) {
        f.delete();
      }
    }
  }

}
