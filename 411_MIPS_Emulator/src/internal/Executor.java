package internal;

public class Executor {
	
	private int PC;
	boolean stopped; //Have we hit a stop instruction?
	
	private enum Type {R, I, J, HALT};
	
	public Executor() {
		PC = 0;
		stopped = false;
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
		bf <<=  31 - h;    //horrifically expensive
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
	
	
	public void operate(byte[] instructions, int[] memory, int[] registers) {
		if(isStopped())
			return;
		
		//fetch!
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
		
		//execute!
		//execute(instr_type, args);
		if(instr_type == Type.HALT)
			stopped = true;
		
		assert Boolean.TRUE;
		
		PC += 4;
	}
}
