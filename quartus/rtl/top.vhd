--   __   __     __  __     __         __
--  /\ "-.\ \   /\ \/\ \   /\ \       /\ \
--  \ \ \-.  \  \ \ \_\ \  \ \ \____  \ \ \____
--   \ \_\\"\_\  \ \_____\  \ \_____\  \ \_____\
--    \/_/ \/_/   \/_____/   \/_____/   \/_____/
--   ______     ______       __     ______     ______     ______
--  /\  __ \   /\  == \     /\ \   /\  ___\   /\  ___\   /\__  _\
--  \ \ \/\ \  \ \  __<    _\_\ \  \ \  __\   \ \ \____  \/_/\ \/
--   \ \_____\  \ \_____\ /\_____\  \ \_____\  \ \_____\    \ \_\
--    \/_____/   \/_____/ \/_____/   \/_____/   \/_____/     \/_/
--
-- https://joshbassett.info
-- https://twitter.com/nullobject
-- https://github.com/nullobject
--
-- Copyright (c) 2020 Josh Bassett
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy
-- of this software and associated documentation files (the "Software"), to deal
-- in the Software without restriction, including without limitation the rights
-- to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
-- copies of the Software, and to permit persons to whom the Software is
-- furnished to do so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in all
-- copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
-- SOFTWARE.

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

library pll;

entity top is
  port (
    clk : in std_logic;
    key : in std_logic_vector(1 downto 0);
    video_csync : out std_logic;
    video_r, video_g, video_b : out std_logic_vector(5 downto 0)
  );
end top;

architecture arch of top is
  component Main is
    port (
      clock          : in std_logic;
      reset          : in std_logic;
      io_video_hSync : out std_logic;
      io_video_vSync : out std_logic;
      io_rgb_r       : out std_logic_vector(3 downto 0);
      io_rgb_g       : out std_logic_vector(3 downto 0);
      io_rgb_b       : out std_logic_vector(3 downto 0)
    );
  end component Main;

  signal clk_video : std_logic;
  signal locked : std_logic;
  signal hsync, vsync : std_logic;
  signal r, g, b : std_logic_vector(3 downto 0);
begin
  pll_inst : entity pll.pll
  port map (
    refclk   => clk,
    rst      => not key(0),
    outclk_0 => clk_video,
    locked   => locked
  );

  main_inst : component Main
  port map (
    clock          => clk_video,
    reset          => not locked,
    io_video_hSync => hsync,
    io_video_vSync => vsync,
    io_rgb_r       => r,
    io_rgb_g       => g,
    io_rgb_b       => b
  );

  video_csync <= not (hsync xor vsync);
  video_r <= r & r(3 downto 2);
  video_g <= g & g(3 downto 2);
  video_b <= b & b(3 downto 2);
end arch;
