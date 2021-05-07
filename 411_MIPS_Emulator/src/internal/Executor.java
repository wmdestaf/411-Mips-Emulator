package internal;

public class Executor {
	
	private int PC;
	boolean stopped; //Have we hit a stop instruction / execution trap?
	
	private enum Type {R, I, J, HALT};
	private enum Trap {SEGFAULT, UNKOP};
	private enum Wmode {
		MODIFY_REGISTER, MODIFY_PC, MODIFY_MEMORY, MODIFY_NOTHING;
		public static final Wmode[] values = values(); //cache trick from https://stackoverflow.com/questions/609860/convert-from-enum-ordinal-to-enum-type/19277247#19277247
	};
	//We'll define 0 as a 16 bit immediate jump, and 1 as a 26 bit immediate jump for the PC
	
	public Executor() {
		PC = 0;
		stopped = false;
	}
	
	private void trap(Trap t) {
		System.err.println("Execution Halted - " + t.toString() + ". PC = " + PC);
		stopped = true;
	}
	
	public boolean isStopped() {
		return stopped;
	}
	
	private Type getType(byte high8) {
		//consequence of java not having byte arithmetic...
		byte opcode = (byte)((high8 & 0xFF) >>> 2); 
		switch(opcode) {
			case 0x00: return Type.R;
			case 0x02: return Type.J;
			case 0x3f: return Type.HALT;
			default:   return Type.I;
		}
	}
	
	private static int load_int_bf(byte ...args) {
		if(args.length != 4)
			throw new IllegalArgumentException("invalid size of bitfield: " + args.length);
		int n = 0;
		for(int i = 0; i < args.length; ++i) {
			n |= args[i];
			n <<= i != args.length - 1 ? 8 : 0;
		}
		return n;
	}
	
	private static int get_bits_inclusive(int bf, int h, int l) {
		if(h < 0 || h > 31 || l < 0 || l > 31 || h < l)
			throw new IllegalArgumentException();
		bf <<=  31 - h;    //I imagine this could be done better.
		bf >>>= 31 - h + l;
		return bf;
	}
	
	int[] decode(Type instr_type, byte raw1, byte raw2, byte raw3, byte raw4) {
		int[] argv;
		int bf = load_int_bf(raw1, raw2, raw3, raw4);
		
		if(instr_type == Type.R) {
			argv = new int[6];      //opcode, rs, rt, rd, shamt, funct
			argv[0] = get_bits_inclusive(bf, 31, 26);
			argv[1] = get_bits_inclusive(bf, 25, 21);
			argv[2] = get_bits_inclusive(bf, 20, 16);
			argv[3] = get_bits_inclusive(bf, 15, 11);
			argv[4] = get_bits_inclusive(bf, 10,  6);
			argv[5] = get_bits_inclusive(bf,  5,  0);
		}
		else if(instr_type == Type.I) {
			argv = new int[4];      //opcode, rs, rt, IMM/16
			argv[0] = get_bits_inclusive(bf, 31, 26);
			argv[1] = get_bits_inclusive(bf, 25, 21);
			argv[2] = get_bits_inclusive(bf, 20, 16);
			argv[3] = get_bits_inclusive(bf, 15,  0);
		}
		else {
			argv = new int[2];    //opcode, dest
			argv[0] = get_bits_inclusive(bf, 31, 26);
			argv[1] = get_bits_inclusive(bf, 25,  0);
		}
		
		return argv;
	}
	
	/*
	 * return [position, value to store, where to store it]
	 */
	private int[] execute(Type instr_type, int[] args, int[] memory, int[] registers) {
		if(instr_type == Type.R) {
			//argument fetch
			
			//trap registers to [0-31]
			if(Integer.compareUnsigned(args[1], 31) > 0 || Integer.compareUnsigned(args[2], 31) > 0 || Integer.compareUnsigned(args[3], 31) > 0) {
				trap(Trap.SEGFAULT);
				return null;
			}
			int rs_v = registers[args[1]]; //rs
			int rt_v = registers[args[2]]; //rt
			
			switch(args[5]) { //func
				case 0x21: return new int[] {args[3],   rs_v + rt_v,  Wmode.MODIFY_REGISTER.ordinal()}; //addu
				case 0x23: return new int[] {args[3],   rs_v - rt_v,  Wmode.MODIFY_REGISTER.ordinal()}; //subu
				case 0x24: return new int[] {args[3],   rs_v & rt_v,  Wmode.MODIFY_REGISTER.ordinal()}; //and
				case 0x25: return new int[] {args[3],   rs_v | rt_v,  Wmode.MODIFY_REGISTER.ordinal()}; //or
				case 0x27: return new int[] {args[3], ~(rs_v | rt_v), Wmode.MODIFY_REGISTER.ordinal()}; //nor
				default:   trap(Trap.UNKOP);
						   return null;
			}
		}
		else if(instr_type == Type.I) {
			//argument fetch
			
			//trap registers to [0-31]
			if(Integer.compareUnsigned(args[1], 31) > 0 || Integer.compareUnsigned(args[2], 31) > 0) {
				trap(Trap.SEGFAULT);
				return null;
			}
			
			int rs_v = registers[args[1]]; //rs
			int rt_v = registers[args[2]]; //rt
			int imm  = args[3];
			
			switch(args[0]) {
				//arithmetic
				case 0x09: return new int[] {args[2], rs_v + imm, Wmode.MODIFY_REGISTER.ordinal()};       //addiu
				
				//branching
				case 0x04: if(rt_v - rs_v == 0) //beq
						       return new int[] {0, imm, Wmode.MODIFY_PC.ordinal()}; 
						   else
					           return new int[] {0, 0,   Wmode.MODIFY_NOTHING.ordinal()};
				
				//data IO
				case 0x23: int mp = imm + rs_v; //lw
						   if(Integer.compareUnsigned(mp, 31) > 0) {
							   trap(Trap.SEGFAULT);
							   return null;
						   }
						   int m = memory[mp];
						   return new int[] {args[2], m, Wmode.MODIFY_REGISTER.ordinal()};
				case 0x2b: mp = imm + rs_v; //sw
				   		   if(Integer.compareUnsigned(mp, 31) > 0) {
				   			   trap(Trap.SEGFAULT);
				   			   return null;
				   		   }
				   		   return new int[] {mp, rt_v, Wmode.MODIFY_MEMORY.ordinal()};
				 default:  trap(Trap.UNKOP);
				 		   return null;
			}
		}
		else if(instr_type == Type.J) {
			switch(args[0]) { 
				case 0x02: return new int[] {1, args[1], Wmode.MODIFY_PC.ordinal()}; //J
				default:   trap(Trap.UNKOP);
						   return null; 
			}
		}
		else //hit STOP instruction
			stopped = true;
		return null;
	}
	
	
	public void operate(byte[] instructions, int[] memory, int[] registers) {
		if(isStopped())
			return;
		
		//fetch!
		//the result of a conditional or unconditional jump may put PC in a bad area
		if(Integer.compareUnsigned(PC, instructions.length) >= 0) {
			trap(Trap.SEGFAULT);
			return;
		}
		
		byte raw1 = instructions[PC];
		byte raw2 = instructions[PC + 1];
		byte raw3 = instructions[PC + 2];
		byte raw4 = instructions[PC + 3];
		
		//decode!
		Type instr_type = getType(raw1);
		if(instr_type == Type.HALT) {
			stopped = true;
			return;
		}
		int[] args = decode(instr_type, raw1, raw2, raw3, raw4);
		
		//execute
		int[] mods = execute(instr_type, args, memory, registers);
		if(mods == null) //hit a trap / HALT
			return;
		
		
		//store / modify PC
		Wmode mode = Wmode.values[mods[2]];

		switch(mode) {
			case MODIFY_REGISTER: registers[mods[0]] = mods[1]; 
								  PC += 4;
								  break;
			case MODIFY_MEMORY:   memory[mods[0]]    = mods[1];
								  PC += 4;
								  break;
			case MODIFY_PC:       PC += 4;
								  if(mods[0] == 0) //treat imm16s and imm26's differently.
									  PC += mods[1] << 2;
								  else {
									  PC &= 0xF0000000;
									  PC |= mods[1] << 2;
								  }
								  break;
			case MODIFY_NOTHING:  break;
		}
	}
}
