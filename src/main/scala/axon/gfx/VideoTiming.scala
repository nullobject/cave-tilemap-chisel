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

package axon.gfx

import axon.types._
import axon.util.Counter
import chisel3._

/** Represents the analog video signals. */
class VideoIO extends Bundle {
  /** Asserted when the pixel clock is enabled */
  val pixelClockEnable = Output(Bool())
  /** Beam position */
  val pos = Output(new UVec2(9))
  /** Horizontal sync */
  val hSync = Output(Bool())
  /** Vertical sync */
  val vSync = Output(Bool())
  /** Horizontal blank */
  val hBlank = Output(Bool())
  /** Vertical blank */
  val vBlank = Output(Bool())
  /** Asserted when the beam is in the display region */
  val enable = Output(Bool())
}

object VideoIO {
  def apply() = new VideoIO
}

/**
 * Represents the video timing configuration.
 *
 * @param clockFreq   The video clock frequency (Hz).
 * @param clockDiv    The video clock divider.
 * @param hFreq       The horizontal frequency (Hz).
 * @param hDisplay    The horizontal display width.
 * @param hFrontPorch The width of the horizontal front porch region.
 * @param hRetrace    The width of the horizontal retrace region.
 * @param hOffset     The horizontal offset (in pixels) of the beam position.
 * @param hInit       The initial horizontal position (for testing).
 * @param vFreq       The vertical frequency (Hz).
 * @param vDisplay    The vertical display height.
 * @param vFrontPorch The width of the vertical front porch region.
 * @param vRetrace    The width of the vertical retrace region.
 * @param vOffset     The vertical offset (in pixels) of the beam position.
 * @param vInit       The initial vertical position (for testing).
 */
case class VideoTimingConfig(clockFreq: Double,
                             clockDiv: Int,
                             hFreq: Double,
                             hDisplay: Int,
                             hFrontPorch: Int,
                             hRetrace: Int,
                             hOffset: Int = 0,
                             hInit: Int = 0,
                             vFreq: Double,
                             vDisplay: Int,
                             vFrontPorch: Int,
                             vRetrace: Int,
                             vOffset: Int = 0,
                             vInit: Int = 0) {
  /** Total width in pixels */
  val width = math.ceil(clockFreq / clockDiv / hFreq).toInt
  /** Total height in pixels */
  val height = math.ceil(hFreq / vFreq).toInt
}

/**
 * Generates the video timing signals required for driving a 15kHz CRT.
 *
 * The horizontal sync signal tells the CRT when to start a new scanline, and the vertical sync
 * signal tells it when to start a new field.
 *
 * The blanking signals indicate whether the beam is in either the horizontal or vertical blanking
 * regions. Video output is disabled while the beam is in these regions.
 *
 * @param config The video timing configuration.
 */
class VideoTiming(config: VideoTimingConfig) extends Module {
  val io = IO(new Bundle {
    /** CRT offset */
    val offset = Input(new SVec2(4))
    /** Video port */
    val video = Output(VideoIO())
  })

  // Counters
  val (_, clockDivWrap) = Counter.static(config.clockDiv)
  val (x, xWrap) = Counter.static(config.width, enable = clockDivWrap, init = config.hInit)
  val (y, yWrap) = Counter.static(config.height, enable = clockDivWrap && xWrap, init = config.vInit)

  // Horizontal regions
  val hBeginDisplay = (config.width.S - config.hDisplay.S - config.hFrontPorch.S - config.hRetrace.S + io.offset.x).asUInt
  val hEndDisplay = (config.width.S - config.hFrontPorch.S - config.hRetrace.S + io.offset.x).asUInt
  val hBeginSync = config.width.U - config.hRetrace.U
  val hEndSync = config.width.U

  // Vertical regions
  val vBeginDisplay = (config.height.S - config.vDisplay.S - config.vFrontPorch.S - config.vRetrace.S + io.offset.y).asUInt
  val vEndDisplay = (config.height.S - config.vFrontPorch.S - config.vRetrace.S + io.offset.y).asUInt
  val vBeginSync = config.height.U - config.vRetrace.U
  val vEndSync = config.height.U

  // Offset the position vector so the display region begins at the origin (i.e. (0, 0))
  val pos = UVec2(x - hBeginDisplay, y - vBeginDisplay)

  // Sync signals
  val hSync = x >= hBeginSync && x < hEndSync
  val vSync = y >= vBeginSync && y < vEndSync

  // Blanking signals
  val hBlank = x < hBeginDisplay || x >= hEndDisplay
  val vBlank = y < vBeginDisplay || y >= vEndDisplay

  // Outputs
  io.video.pixelClockEnable := clockDivWrap
  io.video.pos := pos
  io.video.hSync := hSync
  io.video.vSync := vSync
  io.video.hBlank := hBlank
  io.video.vBlank := vBlank
  io.video.enable := !(hBlank || vBlank)
}
