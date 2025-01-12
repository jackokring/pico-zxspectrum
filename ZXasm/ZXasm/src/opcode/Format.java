package opcode;

import asm.Main;
import literal.Address;
import register.Register;
import register.Register16;
import register.Register8;

@FunctionalInterface
interface Assembler {
    byte[] assemble(Opcode op, int org);
}
public enum Format {

    EX_RR_2((opcode, org) -> {// ex sp/bc, de/hl
        if (opcode.args.size() != 2) return null;//invalid
        int dest = getRegister16(opcode, 0).reg16.ordinal();
        if(dest != Register16.SP.ordinal() || dest != Register16.BC.ordinal()) return null;
        if(dest == Register16.SP.ordinal()) dest = Register16.DE.ordinal();//normal 0/1
        dest = (dest + 1) & 1;//invert
        int source = getRegister16(opcode, 1).reg16.ordinal() - Register16.DE.ordinal();
        if(source > 1) return null;
        source = (source + 1) & 1;//invert
        byte[] ok = new byte[2];
        ok[0] = (byte)0xfd;
        ok[1] = (byte)0x54;
        ok[1] += (dest << 3);//bc/sp
        ok[1] += source;//hl/de
        return allowedIXIYED(opcode, 0, allowedIXIYED(opcode, 1, ok));
    }),
    LD_INDIRECT_RR_R((opcode, org) -> {
        if (opcode.args.size() != 2) return null;//invalid
        int dest = getRegister16(opcode, 0).reg16.ordinal() - Register16.IND_BC.ordinal();
        if(dest > 1) return null;
        int source = getRegister8(opcode, 1).reg8.ordinal();
        if(source > 5) return null;
        byte[] ok = new byte[2];
        ok[0] = (byte)0xfd;
        ok[1] = (byte)0x80;
        ok[1] += (dest << 4);//bc/de
        ok[1] += source;
        return allowedIXIYED(opcode, 0, allowedIXIYED(opcode, 1, ok));
    }),
    LD_R_INDIRECT_RR((opcode, org) -> {
        if (opcode.args.size() != 2) return null;//invalid
        int dest = getRegister8(opcode, 1).reg8.ordinal();
        if(dest > 5) return null;
        int source = getRegister16(opcode, 0).reg16.ordinal() - Register16.IND_BC.ordinal();
        if(source > 1) return null;
        byte[] ok = new byte[2];
        ok[0] = (byte)0xfd;
        ok[1] = (byte)0x88;
        ok[1] += (source << 4);//bc/de
        ok[1] += dest;
        return allowedIXIYED(opcode, 0, allowedIXIYED(opcode, 1, ok));
    }),
    R_N((opcode, org) -> {
        if(opcode.args.size() != 2) return null;//invalid
        int baseOpcode;
        switch(opcode.mnemonic()) {
            case ADD: baseOpcode = 0x64; break;
            case ADC: baseOpcode = 0x6c; break;
            case SUB: baseOpcode = 0x74; break;
            case SBC: baseOpcode = 0x7c; break;
            case AND: baseOpcode = 0xa4; break;
            case XOR: baseOpcode = 0xac; break;
            case OR: baseOpcode = 0xb4; break;
            case CP: baseOpcode = 0xbc; break;
            default: return null;
        }
        int source = getRegister8(opcode, 0).reg8.ordinal() - Register8.H.ordinal();
        if(source > 2) return null;
        baseOpcode += source;
        byte[] ok = new byte[2];
        ok[0] = (byte)0xed;
        ok[1] = (byte)baseOpcode;
        return allowedIXIYED(opcode, 0, ok);
    }),//alu reg, n
    RR((opcode, org) -> {
        if(opcode.args.size() != 1) return null;//invalid
        byte[] ok = opcode.mnemonic().cloneOpcode();
        int dest = getRegister16(opcode, 0).reg16.ordinal();
        if(dest > 2) return null;//no sp either
        ok[1] |= (dest << 8);//by eights
        return ok;
    }),//jpj/jpc
    NUL_1((opcode, org) -> {
        if(opcode.args.size() != 0) return null;//invalid
        return opcode.mnemonic().cloneOpcode();
    }),
    MUL_DE((opcode, org) -> {
        if(opcode.args.size() != 1) return null;//invalid
        if(getRegister16(opcode, 0).reg16 != Register16.DE) return null;
        return opcode.mnemonic().cloneOpcode();
    }),
    IN_AN((opcode, org) -> {
        if(opcode.args.size() != 2) return null;//invalid
        if(getRegister8(opcode, 0).reg8 != Register8.A) return null;
        int baseOpcode = opcode.mnemonic().baseOpcode[0];
        byte[] ok = new byte[2];
        ok[0] = (byte)baseOpcode;
        ok[1] = getLower(opcode, 1);
        if(getUpper(opcode, 1) != 0) return null;
        return ok;
    }),
    OUT_NA((opcode, org) -> {
        if(opcode.args.size() != 2) return null;//invalid
        if(getRegister8(opcode, 1).reg8 != Register8.A) return null;
        int baseOpcode = opcode.mnemonic().baseOpcode[0];
        byte[] ok = new byte[2];
        ok[0] = (byte)baseOpcode;
        ok[1] = getLower(opcode, 0);
        if(getUpper(opcode, 0) != 0) return null;
        return ok;
    }),
    ALU_R((opcode, org) -> {
        if(opcode.args.size() != 1) return null;//invalid
        int baseOpcode;
        switch(opcode.mnemonic()) {
            case ADD: baseOpcode = 0x80; break;
            case ADC: baseOpcode = 0x88; break;
            case SUB: baseOpcode = 0x90; break;
            case SBC: baseOpcode = 0x98; break;
            case AND: baseOpcode = 0xa0; break;
            case XOR: baseOpcode = 0xa8; break;
            case OR: baseOpcode = 0xb0; break;
            case CP: baseOpcode = 0xb8; break;
            default: return null;
        }
        int source = getRegister8(opcode, 0).reg8.ordinal();
        if(source > 7) return null;
        baseOpcode |= source;
        byte[] ok = new byte[1];
        ok[0] = (byte)baseOpcode;
        return withIXIY8(opcode, 0, ok);
    }),
    ALU_N((opcode, org) -> {
        if(opcode.args.size() != 1) return null;//invalid
        int baseOpcode;
        switch(opcode.mnemonic()) {
            case ADD: baseOpcode = 0xc6; break;
            case ADC: baseOpcode = 0xce; break;
            case SUB: baseOpcode = 0xd6; break;
            case SBC: baseOpcode = 0xde; break;
            case AND: baseOpcode = 0xe6; break;
            case XOR: baseOpcode = 0xee; break;
            case OR: baseOpcode = 0xf6; break;
            case CP: baseOpcode = 0xfe; break;
            default: return null;
        }
        byte[] ok = new byte[2];
        ok[0] = (byte)baseOpcode;
        ok[1] = getLower(opcode, 0);//N
        if(getUpper(opcode, 0) != 0) return null;//doesn't fit
        return ok;
    }),
    ID_R((opcode, org) -> {
        if(opcode.args.size() != 1) return null;//invalid
        int baseOpcode = opcode.mnemonic().baseOpcode[0];
        int source = getRegister8(opcode, 0).reg8.ordinal();
        if(source > 7) return null;
        baseOpcode |= source << 3;// by eights
        byte[] ok = new byte[1];
        ok[0] = (byte)baseOpcode;
        return withIXIY8(opcode, 0, ok);
    }),
    ID_RR((opcode, org) -> {
        if(opcode.args.size() != 1) return null;//invalid
        int baseOpcode = opcode.mnemonic().baseDouble;
        int source = getRegister16(opcode, 0).reg16.ordinal();
        if(source > 3) return null;
        baseOpcode |= source << 4;// by eights
        byte[] ok = new byte[1];
        ok[0] = (byte)baseOpcode;
        return withIXIY16(opcode, 0, ok);
    }),
    PP_RR((opcode, org) -> {
        if(opcode.args.size() != 1) return null;//invalid
        int baseOpcode = opcode.mnemonic().baseOpcode[0];
        if(getRegister16(opcode, 0).reg16 == Register16.SP) return null;
        if(getRegister16(opcode, 0).reg16 == Register16.AF)
            getRegister16(opcode, 0).reg16 = Register16.SP;
        int source = getRegister16(opcode, 0).reg16.ordinal();
        if(source > 3) return null;
        baseOpcode |= source << 4;// by 16
        byte[] ok = new byte[1];
        ok[0] = (byte)baseOpcode;
        return withIXIY16(opcode, 0, ok);
    }),
    SP_HL((opcode, org) -> {
        if(opcode.args.size() != 2) return null;//invalid
        int baseOpcode = 0xf9;//hardwired
        if(getRegister16(opcode, 0).reg16 != Register16.SP) return null;
        if(getRegister16(opcode, 1).reg16 != Register16.HL) return null;
        byte[] ok = new byte[1];
        ok[0] = (byte)baseOpcode;
        return withIXIY16(opcode, 1, ok);
    }),
    JP_HL((opcode, org) -> {
        if(opcode.args.size() != 1) return null;//invalid
        int baseOpcode = 0xe9;//hardwired
        if(getRegister8(opcode, 0).reg8 != Register8.IND_HL) return null;
        byte[] ok = new byte[1];
        ok[0] = (byte)baseOpcode;
        return withIXIY16(opcode, 0, ok);//as no offset on ix, iy
    }),
    LD_RR_NN((opcode, org) -> {
        if(opcode.args.size() != 2) return null;//invalid
        int baseOpcode = opcode.mnemonic().baseOpcode[0];
        int dest = getRegister16(opcode, 0).reg16.ordinal();
        if(dest > 3) return null;
        baseOpcode |= dest << 4;// by 16
        if(isIndirect(opcode, 1)) return null;
        byte[] ok = new byte[3];
        ok[0] = (byte)baseOpcode;
        ok[1] = getLower(opcode, 1);
        ok[2] = getUpper(opcode, 1);
        return withIXIY16(opcode, 0, ok);
    }),
    INDIRECT_NN_R((opcode, org) -> {
        if(opcode.args.size() != 2) return null;//invalid
        int baseOpcode = opcode.mnemonic().baseOpcode[0];
        if(!isIndirect(opcode, 0)) return null;//not indirect
        if(getRegister16(opcode, 1).reg16 != Register16.HL
            || getRegister8(opcode, 1).reg8 != Register8.A) return null;
        if(getRegister16(opcode, 1).reg16 == Register16.HL) {
            // ok
        }
        if(getRegister8(opcode, 1).reg8 == Register8.A) {
            baseOpcode |= 16;
        }
        byte[] ok = new byte[3];
        ok[0] = (byte)baseOpcode;
        ok[1] = getLower(opcode, 0);
        ok[2] = getUpper(opcode, 0);
        return withIXIY16(opcode, 1, ok);
    }),
    R_INDIRECT_NN((opcode, org) -> { if(opcode.args.size() != 2) return null;//invalid
        int baseOpcode = opcode.mnemonic().baseDouble;
        if(!isIndirect(opcode, 1)) return null;//not indirect
        if(getRegister16(opcode, 0).reg16 != Register16.HL
                || getRegister8(opcode, 0).reg8 != Register8.A) return null;
        if(getRegister16(opcode, 0).reg16 == Register16.HL) {
            // ok
        }
        if(getRegister8(opcode, 0).reg8 == Register8.A) {
            baseOpcode |= 16;
        }
        byte[] ok = new byte[3];
        ok[0] = (byte)baseOpcode;
        ok[1] = getLower(opcode, 1);
        ok[2] = getUpper(opcode, 1);
        return withIXIY16(opcode, 0, ok);
    }),
    INDIRECT_RR_R((opcode, org) -> {
        if(opcode.args.size() != 2) return null;//invalid
        int baseOpcode = opcode.mnemonic().baseOpcode[0] - 32;//ok
        if(getRegister8(opcode, 1).reg8 != Register8.A) return null;
        if(getRegister16(opcode, 0).reg16 != Register16.IND_BC
            || getRegister16(opcode, 0).reg16 != Register16.IND_DE) {
            return null;
        }
        if(getRegister8(opcode, 0).reg16 == Register16.IND_DE) {
            baseOpcode |= 16;
        }
        byte[] ok = new byte[1];
        ok[0] = (byte)baseOpcode;
        return ok;
    }),
    R_INDIRECT_RR((opcode, org) -> { if(opcode.args.size() != 2) return null;//invalid
        int baseOpcode = opcode.mnemonic().baseDouble - 32;//ok
        if(getRegister8(opcode, 0).reg8 != Register8.A) return null;
        if(getRegister16(opcode, 1).reg16 != Register16.IND_BC
                || getRegister16(opcode, 1).reg16 != Register16.IND_DE) {
            return null;
        }
        if(getRegister8(opcode, 1).reg16 == Register16.IND_DE) {
            baseOpcode |= 16;
        }
        byte[] ok = new byte[3];
        ok[0] = (byte)baseOpcode;
        return ok;
    }),
    LD_R_R((opcode, org) -> {
        if(opcode.args.size() != 2) return null;//invalid
        if(getRegister8(opcode, 0).reg8 == Register8.IND_HL
            && getRegister8(opcode, 1).reg8 == Register8.IND_HL)
                return null;//halt
        int baseOpcode = 0x40;
        int source = getRegister8(opcode, 1).reg8.ordinal();
        if(source > 7) return null;
        int dest = getRegister8(opcode, 0).reg8.ordinal();
        if(dest > 7) return null;
        dest <<= 3;
        source |= dest;
        baseOpcode |= source;
        byte[] ok = new byte[1];
        ok[0] = (byte)baseOpcode;
        return withIXIY8(opcode, -1, ok);// -1 implies either of 0 or 1
    }),
    ADD_RR_RR((opcode, org) -> {
        if(opcode.args.size() != 2) return null;//invalid
        int baseOpcode = opcode.mnemonic().baseOpcode[0];
        if(getRegister16(opcode, 0).reg16 != Register16.HL) return null;//must be hl
        int source = getRegister16(opcode, 1).reg16.ordinal();
        if(source > 3) return null;
        baseOpcode |= source << 4;// by 16
        byte[] ok = new byte[1];
        ok[0] = (byte)baseOpcode;
        return withIXIY16(opcode, 0, ok);
    }),
    RR_RR((opcode, org) -> {
        if(opcode.args.size() != 2) return null;
        if(getRegister16(opcode, 0).reg16 != Register16.HL) return null;
        byte[] ok = opcode.mnemonic().cloneOpcode();
        int dest = getRegister16(opcode, 1).reg16.ordinal();
        if(dest > 3) return null;
        ok[1] |= (dest << 4);
        return allowedIXIYED(opcode, 0, allowedIXIYED(opcode, 1, ok));
    }),
    EX_RR((opcode, org) -> {// ex af
        if(opcode.args.size() != 1) return null;//invalid
        if(getRegister16(opcode, 0).reg16 != Register16.AF) return null;
        return opcode.mnemonic().cloneOpcode();
    }),
    EX_IXY_HL((opcode, org) -> {// ex ix/iy, hl
        if (opcode.args.size() != 2) return null;//invalid
        if (getRegister16(opcode, 1).reg16 != Register16.HL) return null;//must be hl
        if(getRegister16(opcode, 1).hasIXIY()) return null;//must be hl plain
        if (getRegister16(opcode, 0).reg16 != Register16.HL) return null;//must be ix or iy
        if(!getRegister16(opcode, 0).hasIXIY()) return null;//must be ix iy
        int baseOpcode = 0xed;//an ED with a prefix IX or IY
        //ok
        byte[] ok = new byte[1];
        ok[0] = (byte) baseOpcode;
        return withIXIY16(opcode, 0, ok);
    }),
    EX_RR_RR((opcode, org) -> {// ex xx, hl
        if(opcode.args.size() != 2) return null;//invalid
        if(getRegister16(opcode, 1).reg16 != Register16.HL) return null;
        int baseOpcode = opcode.mnemonic().baseDouble;
        if(getRegister16(opcode, 0).reg16 == Register16.DE) {
            baseOpcode += 8;
            if(getRegister16(opcode, 1).hasIXIY()) return null;//de/hl bit flipper consequence
        } else if(getRegister16(opcode, 0).reg16 != Register16.IND_SP) return null;
        //ok
        byte[] ok = new byte[1];
        ok[0] = (byte)baseOpcode;
        //no IX, IY
        return withIXIY16(opcode, 1, ok);
    }),
    R((opcode, org) -> { // bit ops simplicity
        if(opcode.args.size() == 1) {//normal
            int source = getRegister8(opcode, 0).reg8.ordinal();
            if(source > 8) return null;
            byte[] ok = opcode.mnemonic().cloneOpcode();
            ok[1] |= source;
            return withIXIY8Bit(opcode, 0, ok);
        }
        if(opcode.args.size() != 2) return null;
        // two special
        int dest = getRegister8(opcode, 1).reg8.ordinal();
        if(dest > 7 || dest == Register8.IND_HL.ordinal()) return null;
        if(getRegister8(opcode, 0).reg8 != Register8.IND_HL) return null;
        if(!getRegister8(opcode, 0).hasIXIY()) return null;
        byte[] ok = opcode.mnemonic().cloneOpcode();
        ok[1] |= dest;
        return withIXIY8Bit(opcode, 0, ok);
    }),
    JP_NN((opcode, org) -> {
        if(opcode.args.size() != 1) return null;//invalid
        int baseOpcode = opcode.mnemonic().baseOpcode[0];//for jp, call
        byte[] ok = new byte[3];
        ok[0] = (byte)baseOpcode;
        ok[1] = getLower(opcode, 0);
        ok[2] = getUpper(opcode, 0);
        return ok;
    }),
    D((opcode, org) -> {
        if(opcode.args.size() != 1) return null;//invalid
        int baseOpcode = opcode.mnemonic().baseOpcode[0];//for djnz, jr
        byte[] ok = new byte[2];
        ok[0] = (byte)baseOpcode;
        ok[1] = getOffset(opcode, 0, org);
        return ok;
    }),
    F_D((opcode, org) -> {
        if(opcode.args.size() != 1) return null;//invalid
        int baseOpcode = opcode.mnemonic().baseDouble;//for jr
        int source = getFlags(opcode, 0).flags.ordinal();
        if(source > 3) return null;
        baseOpcode |= source << 3;// 8 per flag
        byte[] ok = new byte[2];
        ok[0] = (byte)baseOpcode;
        ok[1] = getOffset(opcode, 0, org);
        return ok;
    }),//jr f, d
    IM((opcode, org) -> {
        if(opcode.args.size() != 1) return null;//invalid
        byte[] ok = opcode.mnemonic().cloneOpcode();
        if(getUpper(opcode, 0) != 0) return null;
        int adjust = getLower(opcode, 0);
        if(adjust != (adjust & 3)) return null;
        switch (adjust) {
            case 0: ok[1] += 0; break;
            case 1: ok[1] += 16; break;
            case 2: ok[1] += 24; break;
            case 3: ok[1] += 8; break;//IM 3
            default: break;
        }
        return ok;
    }),
    RST((opcode, org) -> {
        if(opcode.args.size() != 1) return null;//invalid
        int baseOpcode = opcode.mnemonic().baseOpcode[0];
        if(getUpper(opcode, 0) != 0) return null;
        int adjust = getLower(opcode, 0);
        if(adjust != (adjust & 0x38)) return null;//bad thing
        baseOpcode += (adjust & 0x38);
        byte[] ok = new byte[1];
        ok[0] = (byte)baseOpcode;
        return ok;
    }),
    JP_F_NN((opcode, org) -> {
        if(opcode.args.size() != 2) return null;//invalid
        int baseOpcode = opcode.mnemonic().baseDouble;//for jp, call
        int source = getFlags(opcode, 0).flags.ordinal();
        if(source > 7) return null;
        baseOpcode |= source << 3;// 8 per flag
        byte[] ok = new byte[3];
        ok[0] = (byte)baseOpcode;
        ok[1] = getLower(opcode, 1);
        ok[2] = getUpper(opcode, 1);
        return ok;
    }),
    RET_F((opcode, org) -> {
        if(opcode.args.size() != 1) return null;//invalid
        int baseOpcode = 0xc0;
        int source = getFlags(opcode, 0).flags.ordinal();
        if(source > 7) return null;
        baseOpcode |= source << 3;// 8 per flag
        byte[] ok = new byte[1];
        ok[0] = (byte)baseOpcode;
        return ok;
    }),
    N_R((opcode, org) -> {
        if(opcode.args.size() == 2) {//normal
            int source = getRegister8(opcode, 1).reg8.ordinal();
            if(source > 7) return null;
            if(getUpper(opcode, 0) != 0) return null;
            int bit = getLower(opcode, 0);
            if(bit != (bit & 7)) return null;
            bit &= 7;
            bit <<= 3;//shift prefix
            byte[] ok = opcode.mnemonic().cloneOpcode();
            ok[1] |= source | bit;
            return withIXIY8Bit(opcode, 1, ok);
        }
        if(opcode.args.size() != 3 || opcode.mnemonic() == Mnemonic.BIT) return null;
        // three special
        int dest = getRegister8(opcode, 2).reg8.ordinal();
        if(dest > 7 || dest == Register8.IND_HL.ordinal()) return null;
        if(getRegister8(opcode, 1).reg8 != Register8.IND_HL) return null;
        if(!getRegister8(opcode, 1).hasIXIY()) return null;
        byte[] ok = opcode.mnemonic().cloneOpcode();
        if(getUpper(opcode, 0) != 0) return null;
        int bit = getLower(opcode, 0);
        if(bit != (bit & 7)) return null;
        bit &= 7;
        bit <<= 3;//shift prefix
        ok[1] |= dest | bit;
        return withIXIY8Bit(opcode, 1, ok);
    }),// bit and register
    RR_INDIRECT_NN((opcode, org) -> {
        if(opcode.args.size() != 2) return null;
        byte[] ok = new byte[4];
        ok[0] = (byte)0xed;
        ok[1] = (byte)0x4b;
        int dest = getRegister16(opcode, 0).reg16.ordinal();
        if(dest > 3) return null;
        ok[1] |= (dest << 4);
        if(!isIndirect(opcode, 1)) return null;
        ok[2] = getLower(opcode, 1);
        ok[3] = getUpper(opcode, 1);
        return allowedIXIYED(opcode, 0, ok);
    }),
    INDIRECT_NN_RR((opcode, org) -> {
        if(opcode.args.size() != 2) return null;
        byte[] ok = new byte[4];
        ok[0] = (byte)0xed;
        ok[1] = (byte)0x43;
        int dest = getRegister16(opcode, 1).reg16.ordinal();
        if(dest > 3) return null;
        ok[1] |= (dest << 4);
        if(!isIndirect(opcode, 0)) return null;
        ok[2] = getLower(opcode, 0);
        ok[3] = getUpper(opcode, 0);
        return allowedIXIYED(opcode, 1, ok);
    }),
    LD_IR((opcode, org) -> {
        if(opcode.args.size() != 2) return null;
        byte[] ok = new byte[2];
        ok[0] = (byte)0xed;
        ok[1] = (byte)0x47;
        if(getRegister8(opcode, 0).reg8 == Register8.A) {
            ok[1] += 16;
            if(getRegister8(opcode, 1).reg8 != Register8.I
               || getRegister8(opcode, 1).reg8 != Register8.R) return null;
            if(getRegister8(opcode, 1).reg8 == Register8.R) ok[1] += 8;
        } else if(getRegister8(opcode, 1).reg8 == Register8.A) {
            if(getRegister8(opcode, 0).reg8 != Register8.I
                    || getRegister8(opcode, 0).reg8 != Register8.R) return null;
            if(getRegister8(opcode, 0).reg8 == Register8.R) ok[1] += 8;
        }
        return ok;
    }),
    R_INDIRECT_R((opcode, org) -> {
        // for in (c)
        if(opcode.args.size() != 2) return null;
        byte[] ok = new byte[2];
        ok[0] = (byte)0xed;
        ok[1] = (byte)0x40;
        if(getRegister8(opcode, 1).reg8 != Register8.IND_C) return null;
        int dest = getRegister8(opcode, 0).reg8.ordinal();
        if(dest > 7 || dest == Register8.IND_HL.ordinal()) return null;
        ok[1] |= (dest << 3);
        return allowedIXIYED(opcode, 0, ok);
    }),
    INDIRECT_R((opcode, org) -> {
        // for in/out (c) specials
        if(opcode.args.size() == 1) {
            byte[] ok = new byte[2];
            ok[0] = (byte)0xed;
            ok[1] = (byte)0x70;
            if(getRegister8(opcode, 0).reg8 != Register8.IND_C) return null;
            if(opcode.mnemonic() != Mnemonic.IN) return null;
            return ok;
        } else if(opcode.args.size() == 2) {
            byte[] ok = new byte[2];
            ok[0] = (byte)0xed;
            ok[1] = (byte)0x71;
            if(getRegister8(opcode, 0).reg8 != Register8.IND_C) return null;
            if(opcode.mnemonic() != Mnemonic.OUT) return null;
            if(getUpper(opcode, 1) != 0 && getLower(opcode, 1) != 0) return null;
            return ok;
        } else return null;
    }),
    INDIRECT_R_R((opcode, org) -> {
        // for out (c)
        if(opcode.args.size() != 2) return null;
        byte[] ok = new byte[2];
        ok[0] = (byte)0xed;
        ok[1] = (byte)0x41;
        if(getRegister8(opcode, 0).reg8 != Register8.IND_C) return null;
        int dest = getRegister8(opcode, 1).reg8.ordinal();
        if(dest > 7 || dest == Register8.IND_HL.ordinal()) return null;
        ok[1] |= (dest << 3);
        return allowedIXIYED(opcode, 1, ok);
    }),
    MACRO((opcode, org) -> { return null; });

    Assembler ass;

    Format(Assembler ass) {
        this.ass = ass;
    }

    public static Register getRegister8(Opcode opcode, int number) {
        Register r = opcode.args.get(number);
        if(r.reg8 == null) {
            //Main.error(Main.Errors.BAD_REGISTER);
            r.reg8 = Register8.BAD_REG;
        }
        return r;
    }

    public static byte getOffset(Opcode opcode, int number, int org) {
        Register r = opcode.args.get(number);
        if(r.data == null) {
            //Main.error(Main.Errors.BAD_REGISTER);
            r.data = new Address(org);//auto loop?
        }
        byte b = (byte)(r.data.address - org);
        if(b != r.data.address - org) {//sign extended
            Main.error(Main.Errors.OFFSET);//error
        }
        return b;
    }

    public static byte getUpper(Opcode opcode, int number) {
        Register r = opcode.args.get(number);
        if(r.data == null) {
            //Main.error(Main.Errors.BAD_REGISTER);
            r.data = new Address(0);//auto loop?
        }
        return (byte)(r.data.address >> 8);
    }

    public static byte getLower(Opcode opcode, int number) {
        Register r = opcode.args.get(number);
        if(r.data == null) {
            //Main.error(Main.Errors.BAD_REGISTER);
            r.data = new Address(0);//auto loop?
        }
        return (byte)(r.data.address);
    }

    public static boolean isIndirect(Opcode opcode, int number) {
        Register r = opcode.args.get(number);
        if(r.data == null) return false;
        return r.data.indirect;
    }

    public static Register getRegister16(Opcode opcode, int number) {
        Register r = opcode.args.get(number);
        if(r.reg16 == null) {
            //Main.error(Main.Errors.BAD_REGISTER);
            r.reg16 = Register16.BAD_REG;
        }
        return r;
    }

    public static Register getFlags(Opcode opcode, int number) {
        Register r = opcode.args.get(number);
        if(r.flags == null) {
            //Main.error(Main.Errors.BAD_REGISTER);
            r.flags = CCode.BAD_FLAG;
        }
        return r;
    }

    public static byte[] withIXIY8(Opcode opcode, int number, byte[] in) {
        if(number == -1) {//either
            //doesn't catch mixing of non IX IY and HL in opcode parameters
            if (getRegister8(opcode, 0).reg8 == Register8.IND_HL)
                return getRegister8(opcode, 0).withIXIY(in);
            if (getRegister8(opcode, 1).reg8 == Register8.IND_HL)
                return getRegister8(opcode, 1).withIXIY(in);
            if(getRegister8(opcode, 0).reg8 == Register8.H
                    || getRegister8(opcode, 0).reg8 == Register8.L)
                return getRegister8(opcode, 0).withIXIYNotIndirect(in);
            if(getRegister8(opcode, 1).reg8 == Register8.H
                    || getRegister8(opcode, 1).reg8 == Register8.L)
                return getRegister8(opcode, 1).withIXIYNotIndirect(in);
        }
        if(getRegister8(opcode, number).reg8 == Register8.IND_HL)
            return getRegister8(opcode, number).withIXIY(in);
        if(getRegister8(opcode, number).reg8 == Register8.H
            || getRegister8(opcode, number).reg8 == Register8.L)
            return getRegister8(opcode, number).withIXIYNotIndirect(in);
        return in;//can't do
    }

    public static byte[] withIXIY8Bit(Opcode opcode, int number, byte[] in) {
        if(getRegister8(opcode, number).reg8 == Register8.IND_HL)
            return getRegister8(opcode, number).withIXIYBit(in);
        return in;
    }

    public static byte[] withIXIY16(Opcode opcode, int number, byte[] in) {
        if(getRegister16(opcode, number).reg16 == Register16.HL)
            return getRegister16(opcode, number).withIXIYNotIndirect(in);
        return in;//can't do
    }

    public static byte[] allowedIXIYED(Opcode opcode, int number, byte[] in) {
        if(in[0] != 0xed) {
            return in;
        }
        Register r = getRegister16(opcode, number);
        if(r.hasIXIY()) return null;
        r = getRegister8(opcode, number);
        if(r.hasIXIY()) return null;
        return in;
    }
}
