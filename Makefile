.PHONY: build test program clean

build:
	sbt compile run
	cd quartus; quartus_sh --flow compile cave

test:
	sbt test

program:
	cd quartus; quartus_pgm -m jtag -c 1 -o "p;output_files/cave.sof@2"

clean:
	rm -rf project/target rtl/cave.* target test_run_dir quartus/db quartus/incremental_db quartus/output_files
