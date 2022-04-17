/*
 *    __   __     __  __     __         __
 *   /\ "-.\ \   /\ \/\ \   /\ \       /\ \
 *   \ \ \-.  \  \ \ \_\ \  \ \ \____  \ \ \____
 *    \ \_\\"\_\  \ \_____\  \ \_____\  \ \_____\
 *     \/_/ \/_/   \/_____/   \/_____/   \/_____/
 *    ______     ______       __     ______     ______     ______
 *   /\  __ \   /\  == \     /\ \   /\  ___\   /\  ___\   /\__  _\
 *   \ \ \/\ \  \ \  __<    _\_\ \  \ \  __\   \ \ \____  \/_/\ \/
 *    \ \_____\  \ \_____\ /\_____\  \ \_____\  \ \_____\    \ \_\
 *     \/_____/   \/_____/ \/_____/   \/_____/   \/_____/     \/_/
 *
 *  https://joshbassett.info
 *  https://twitter.com/nullobject
 *  https://github.com/nullobject
 *
 *  Copyright (c) 2022 Josh Bassett
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package cave

import axon.Util
import axon.gfx._
import axon.mem._
import axon.types._
import chisel3._
import chisel3.util._

class TilemapProcessor extends Module {
  val io = IO(new Bundle {
    /** The tilemap graphics format. */
    val format = Input(UInt(2.W))
    /** Video timing signals. */
    val video = Input(new VideoIO)
    /** Tile ROM read signals. */
    val tileRom = ReadMemIO(Config.TILE_ROM_ADDR_WIDTH, Config.TILE_ROM_DATA_WIDTH)
    /** RGB output signal. */
    val rgb = Output(new RGB(Config.BITS_PER_CHANNEL))
  })

  /** Decodes a row of pixels. */
  def decode(data: Bits): Vec[Bits] = Mux(
    io.format === Config.GFX_FORMAT_8BPP.U,
    VecInit(TilemapProcessor.decode8BPP(data)),
    VecInit(TilemapProcessor.decode4BPP(data))
  )

  val pos = io.video.pos

  // Tilemap column and row
  val col = pos.x(8, 3)
  val row = pos.y(8, 3)

  // Pixel position within tile
  val offset = {
    val x = pos.x(2, 0)
    val y = pos.y(2, 0)
    UVec2(x, y)
  }

  // Control signals
  val latchTile = offset.x === 6.U && io.video.pixelClockEnable
  val latchPixels = offset.x(2, 0) === 7.U && io.video.pixelClockEnable

  val tileCodeReg = RegEnable(row ## (col + 1.U), latchTile)
  val tileRomAddr = tileCodeReg ## offset.y(2, 0)
  val pixels = RegEnable(decode(io.tileRom.dout), latchPixels)

  // Outputs
  io.tileRom.rd := true.B
  io.tileRom.addr := tileRomAddr
  io.rgb := RGB(pixels(pos.x(2, 0)))
}

object TilemapProcessor {
  /**
   * Decodes 8x8x4 tiles (i.e. 32 bits per row)
   *
   * @param data The 32-bit tile ROM data.
   */
  def decode4BPP(data: Bits): Seq[Bits] = {
    Seq(0, 1, 2, 3, 4, 5, 6, 7)
      // Decode data into nibbles
      .reverseIterator.map(Util.decode(data, 8, 4).apply)
      // Pad nibbles into 8-bit pixels
      .map(_.pad(8)).toSeq
  }

  /**
   * Decodes 8x8x8 tiles (i.e. 64 bits per row)
   *
   * @param data The 64-bit tile ROM data.
   */
  def decode8BPP(data: Bits): Seq[Bits] =
    Seq(2, 0, 3, 1, 6, 4, 7, 5, 10, 8, 11, 9, 14, 12, 15, 13)
      // Decode data into nibbles
      .reverseIterator.map(Util.decode(data, 16, 4).apply)
      // Join high/low nibbles into 8-bit pixels
      .grouped(2).map(Cat(_)).toSeq
}
