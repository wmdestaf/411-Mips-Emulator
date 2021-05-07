package ui;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.junit.Test;

public class TestEmulator {

	private void compareFile(String f1, String f2) {
		Scanner s1 = null, s2 = null;
		try {
			s1 = new Scanner(new File(f1));
			s2 = new Scanner(new File(f2));
		}
		catch(FileNotFoundException e) {
			fail(e.getMessage());
		}
		
		while(s1.hasNextLine()) {
			String ss1 = s1.nextLine();
			String ss2 = s2.nextLine();
			assertEquals(ss2, ss1);
		}
		assertFalse(s2.hasNextLine());
	}
	
	public void testProgram(String instrs, String imem, 
			String outmem, String outregs, String eoutmem, String eoutregs) {
		Emulator.main(new String[] {instrs, imem});
		compareFile(outmem,  eoutmem);
		compareFile(outregs, eoutregs);
	}
	
	@Test
	public void testProgram1() {
		testProgram("in_files/Instructions1.txt", "in_files/IMemory.txt", 
				    "Memory.txt", "Registers.txt", 
				    "out_files/Memory1.txt", "out_files/Registers1.txt");
	}
	
	@Test
	public void testProgram2() {
		testProgram("in_files/Instructions2.txt", "in_files/IMemory.txt", 
			    "Memory.txt", "Registers.txt", 
			    "out_files/Memory2.txt", "out_files/Registers2.txt");
	}
	@Test
	public void testProgram3() {
		testProgram("in_files/Instructions3.txt", "in_files/IMemory.txt", 
			    "Memory.txt", "Registers.txt", 
			    "out_files/Memory3.txt", "out_files/Registers3.txt");
	}
}