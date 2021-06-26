/*
 * Copyright 2015 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.termd.core.tty;


import io.termd.core.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TtyOutputMode implements Consumer<int[]> {

  private final Consumer<int[]> readHandler;

  public TtyOutputMode(Consumer<int[]> readHandler) {
    this.readHandler = readHandler;
  }

  @Override
  public void accept(int[] data) {
    if (readHandler != null && data.length > 0) {
      // TODO 起始指针
      int prev = 0;
      // TODO 当前指针
      int ptr = 0;
      while (ptr < data.length) {
        // Simple implementation that works only on system that uses /n as line terminator
        // equivalent to 'stty onlcr'
        int cp = data[ptr];
        if (cp == '\n') {
          if (ptr > prev) {
            // TODO 遇到回车如果当前指针大于起始指针(即回车不再行开头的时候), 发送整行数据，不带回车
            //  也就是回车前面有数据的话，当前回车会被忽略
            sendChunk(data, prev, ptr);
          }
          // TODO 否则发送回车换行
          readHandler.accept(new int[]{'\r','\n'});
          // TODO 发送回车换行后起始指针和当前指针同时移动，因为下次输出是在新行了
          prev = ++ptr;
        } else {
          ptr++;
        }
      }
      if (ptr > prev) {
        sendChunk(data, prev, ptr);
      }
    }
  }

  private void sendChunk(int[] data, int prev, int ptr) {
    int len = ptr - prev;
    int[] buf = new int[len];
    System.arraycopy(data, prev, buf, 0, len);
    readHandler.accept(buf);
  }
}
