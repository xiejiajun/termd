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

package io.termd.core.io;

import io.termd.core.function.Consumer;
import io.termd.core.util.Helper;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BinaryDecoder {

  private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

  private CharsetDecoder decoder;
  private ByteBuffer bBuf;
  private final CharBuffer cBuf;
  private final Consumer<int[]> onChar;

  public BinaryDecoder(Charset charset, Consumer<int[]> onChar) {
    this(2, charset, onChar);
  }

  public BinaryDecoder(int initialSize, Charset charset, Consumer<int[]> onChar) {
    if (initialSize < 2) {
      throw new IllegalArgumentException("Initial size must be at least 2");
    }
    decoder = charset.newDecoder();
    bBuf = EMPTY;
    cBuf = CharBuffer.allocate(initialSize); // We need at least 2
    this.onChar = onChar;
  }

  /**
   * Set a new charset on the decoder.
   *
   * @param charset the new charset
   */
  public void setCharset(Charset charset) {
    decoder = charset.newDecoder();
  }

  public void write(byte[] data) {
    write(data, 0, data.length);
  }

  public void write(byte[] data, int start, int len) {

    // Fill the byte buffer
    // TODO 获取当前ByteBuffer的剩余长度
    int remaining = bBuf.remaining();
    if (len > remaining) {
      // Allocate a new buffer
      ByteBuffer tmp = bBuf;
      int length = tmp.position() + len;
      // TODO 扩容
      bBuf = ByteBuffer.allocate(length);
      // TODO 反转ByteBuffer, 将limit游标设置为当前数据指针position指向的位置，并将当前指针position指向0
      tmp.flip();
      // TODO 将老数据映射到扩容后的ByteBuffer
      bBuf.put(tmp);
    }
    // TODO 将新数据放到ByteBuffer
    bBuf.put(data, start, len);
    // TODO 反转ByteBuffer, 将limit游标设置为当前数据指针position指向的位置，并将当前指针position指向0
    //  这样一来ByteBuffer中的可用数据就是0-limit范围的数据了
    bBuf.flip();

    // Drain the byte buffer
    // TODO 循环读取并清空bBuf
    while (true) {
      IntBuffer iBuf = IntBuffer.allocate(bBuf.remaining());
      // TODO 将bBuf映射到cBuf, endOfInput设置为false，cBuf空间不够时通过多次读取来完成，而不是直接报错
      CoderResult result = decoder.decode(bBuf, cBuf, false);
      // TODO 反转CharBuffer
      cBuf.flip();
      while (cBuf.hasRemaining()) {
        // TODO 高位
        char c = cBuf.get();
        if (isSurrogate(c)) {
          // TODO 汉字
          if (Character.isHighSurrogate(c)) {
            if (cBuf.hasRemaining()) {
              // TODO 低位
              char low = cBuf.get();
              if (Character.isLowSurrogate(low)) {
                int codePoint = Character.toCodePoint(c, low);
                if (Character.isValidCodePoint(codePoint)) {
                  // TODO 高低位组合得到的ascii码
                  iBuf.put(codePoint);
                } else {
                  throw new UnsupportedOperationException("Handle me gracefully");
                }
              } else {
                throw new UnsupportedOperationException("Handle me gracefully");
              }
            } else {
              throw new UnsupportedOperationException("Handle me gracefully");
            }
          } else {
            throw new UnsupportedOperationException("Handle me gracefully");
          }
        } else {
          iBuf.put((int) c);
        }
      }
      // TODO 反转
      iBuf.flip();
      int[] codePoints = new int[iBuf.limit()];
      // TODO iBuf拷贝到codePoints数组
      iBuf.get(codePoints);
      // TODO 发送出去：TtyEventDecoder.accept
      onChar.accept(codePoints);
      // TODO 压缩：将读取过的数据清除，后面的数据往前挪，position往前挪动，limit位置不变
      cBuf.compact();
      if (result.isOverflow()) {
        // TODO bBuf长度超过了cBuf的，上一次没有读取完，再进行下一轮读取
        // We still have work to do
      } else if (result.isUnderflow()) {
        // TODO bBuf的数据长度小于或等于cBuf的，解码完成，break退出本次读取
        if (bBuf.hasRemaining()) {
          // We need more input
          Helper.noop();
        } else {
          // We are done
          Helper.noop();
        }
        break;
      } else {
        // TODO 解码异常，向外报错
        throw new UnsupportedOperationException("Handle me gracefully");
      }
    }
    // TODO 压缩：将读取过的数据清除，后面的数据往前挪，position往前挪动，limit位置不变
    bBuf.compact();
  }

  private static boolean isSurrogate(char ch) {
    return ch >= '\uD800' && ch < ('\uDFFF' + 1);
  }
}
