package ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import internal.Executor;

public class Emulator {
	
	/* for direct indexing without overhead, the primitive type */
	private int[] registers, memory;
	private byte[] instructions;
	
	private static long careful_pow(long a, long b) throws ArithmeticException {
		//this could be improved.
		if(b == 0)
			return 1;
		long sum = 1;
		for(long i = 0; i < b; ++i) {
			sum = Math.multiplyExact(sum, a);
		}
		return sum;
	}
	
	//I'm sure this is possible with generics. 
	private byte[] cutByte(long[] array) {
		byte[] resultant = new byte[array.length];
		for(int i = 0; i < array.length; ++i) {
			long n = array[i];
			if(n > Byte.MAX_VALUE - Byte.MIN_VALUE) {
				System.err.printf("Error: %d to large for specified parameters [min:0,max:%d]",
						n, Byte.MAX_VALUE - Byte.MIN_VALUE);
				System.exit(1);
			}
			resultant[i] = (byte)n;
		}
		return resultant;
	}
	
	private int[] cutInt(long[] array) {
		int[] resultant = new int[array.length];
		for(int i = 0; i < array.length; ++i) {
			long n = array[i];
			if(n > ((long)Integer.MAX_VALUE) - Integer.MIN_VALUE) {
				System.err.printf("Error: %d to large for specified parameters [min:0,max:%d]",
						n, Integer.MAX_VALUE - Integer.MIN_VALUE);
				System.exit(1);
			}
			resultant[i] = (int)n;
		}
		return resultant;
	}
	
	private long[] load_input(String filename, int expected_len, int expected_lines) {
		//Open file
		Scanner sc = null;
		try {
			sc = new Scanner(new File(filename));
		} catch (FileNotFoundException e) {
			System.err.println("Could not read from file: " + filename);
			System.exit(1);
		}
		
		//Load file into memory
		int n = 1;
		ArrayList<String> list = new ArrayList<String>();
		while(sc.hasNext()) { 
			String s = sc.next();
			if(expected_len != 0 && s.length() != expected_len) {
				System.err.printf("On line %d of file %s: Expected Length: %d, got %d\n", 
						n, filename, expected_len, s.length());
				System.exit(1);
			}
			list.add(s);
			++n;
		}
		if(expected_lines != 0 && list.size() != expected_lines) {
			System.err.printf("From file %s: Expected %d lines, got %d\n", 
					filename, expected_lines, list.size());
			System.exit(1);
		}
		else if(expected_lines == 0)
			expected_lines = list.size();
		
		//Parse file into memory
		long[] array = new long[expected_lines];
		for(int line = 0; line < expected_lines; ++line) {
			String c_line = list.get(line); //O(1)
			long sum = 0, pow = -1;
			int l_length = expected_len == 0 ? c_line.length() : expected_len;
			for(int pos = l_length - 1; pos >= 0; --pos) {
				char c = c_line.charAt(pos); //JVM inlines this
				pow++;
				if(c == '0')
					continue;
				else if(c == '1') {
					try {
						long pwr = careful_pow(2, pow);
						sum = Math.addExact(sum, pwr);
					} catch(ArithmeticException e) {
						System.err.printf("From file %s: Experienced overflow on Line %d\n",
								filename, line + 1);
						System.exit(1);
					}
				}
				else {
					System.err.printf("From file %s: Unexpected character on Line %d - %c (%02x)\n",
							filename, line + 1, c, c);
					System.exit(1);
				}
			}
			array[line] = sum;
		}
		
		return array;
	}
	
	private void setupAndRun(String[] args) {
		instructions = cutByte(load_input(args[0], 8, 0));
		memory       = cutInt (load_input(args[1], 32, 32));
		registers    = new int[32];

		Executor e = new Executor();
		do {
			e.operate(instructions, memory, registers);
		} while(!e.isStopped());
		
		assert Boolean.TRUE;
	}
	
	public static void main(String[] args) {
		if(args.length != 2)
			usage();
		new Emulator().setupAndRun(args);
	}
	
	private static void usage() {
		System.err.println("usage: MEmulator <instruction file> <memory file>");
		System.exit(1);
	}
}
