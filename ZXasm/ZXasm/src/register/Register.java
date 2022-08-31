package register;

import literal.Address;
import literal.Label;

public class Register {

    boolean is8;
    boolean isIX, isIY;
    Register8 reg8;
    Register16 reg16;
    byte offset;

    Address data;

    public static Register getRegister(String reg) {
        Register r = Register8.getRegister(reg);
        if(r == null) {
            r = Register16.getRegister(reg);
        }
        if(r == null) {
            try {
                Address a = new Address((char) Integer.parseInt(reg));
                r = new Register(a);
            } catch(Exception e) {
                // leave as null
            }
        }
        if(r == null) {
            r = new Register(Label.findLabel(reg));//could be un-found 0
        }
        return r;
    }

    public Register(Register8 reg) {
        is8 = true;
        reg8 = reg;
    }

    public Register(Register16 reg) {
        reg16 = reg;
    }

    public Register(Register8 reg, boolean ix, boolean iy, byte offset) {
        is8 = true;
        reg8 = reg;
        isIX = ix;
        isIY = iy;
        this.offset = offset;
    }

    public Register(Register16 reg, boolean ix, boolean iy) {
        reg16 = reg;
        isIX = ix;
        isIY = iy;
    }

    public Register(Address address) {
        this.data = address;
    }
}
