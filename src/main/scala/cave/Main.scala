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
 *  Copyright (c) 2020 Josh Bassett
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

import axon.gfx._
import axon.mem._
import axon.types._
import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}

/** This is the top-level module. */
class Main extends Module {
  val io = IO(new Bundle {
    /** Video signals */
    val video = VideoIO()
    /** RGB output */
    val rgb = Output(new RGB(Config.BITS_PER_CHANNEL))
  })

  val videoTiming = Module(new VideoTiming(Config.videoTimingConfig))
  videoTiming.io.offset := SVec2.zero
  videoTiming.io.video <> io.video

  val rom = Module(new SinglePortRom(
    addrWidth = Config.TILE_ROM_ADDR_WIDTH,
    dataWidth = Config.TILE_ROM_DATA_WIDTH,
    depth = 16384,
    initFile = "roms/tiles.mif"
  ))

  val tilemap = Module(new TilemapProcessor)
  tilemap.io.tileSize := true.B
  tilemap.io.tileFormat := Config.GFX_FORMAT_4BPP.U
  tilemap.io.video <> videoTiming.io.video
  tilemap.io.tileRom <> rom.io

  // Outputs
  io.rgb := Mux(videoTiming.io.video.enable, tilemap.io.rgb, RGB(0.U(Config.BITS_PER_CHANNEL.W)))
}

object Main extends App {
  (new ChiselStage).execute(
    Array("--compiler", "verilog", "--target-dir", "quartus/rtl", "--output-file", "Main"),
    Seq(ChiselGeneratorAnnotation(() => new Main()))
  )
}
