//===========================================
// Z80X new instructions too
//===========================================
// Write it how you want it
// The following code is to build a template for making all manor
// of assemblies for the Z80X processor extensions
// the syntax is decided for the Java ZXASM project and used throughout.

// Partially wrote by Simon Jackson @jackokring github.

  org $0000
bankPort:     equ $7ffd         // 128k banking port
portPlusA:    equ $1ffd         // +2A/+3A port for extra ROMs

main:
.rest00:
  di
  ld sp, stack. //"." for last before next symbol as stack grows down
  im 2
  jr .start
  upto $08
.rst08:
  nop         // interface I error intercept
.saveReg:
  ex (sp), hl
  push de
  push bc
  push af
  push hl
  clc
  ret
  upto $10
.rst10:        // warm start
  jr .warm
.loadReg:
  pop hl
  pop af
  pop bc
  pop de
  ex (sp), hl
  ret
  upto $18
.rst18:
  upto $20
.rst20:
  upto $28
.rst28:
  upto $30
.rst30:
  upto $38
.rst38:          // interrupt IM 2
  call userInt              //3
  call sysInt               //3
  ret                       //1 (7 bytes)
.spectrum:
  ld bc, portPlusA           //3
  ld a, 4                    //2
  out (c), a                 //2
  ld bc, bankPort            //3
  ld a, 16                   //2
  out (c), a                 //2
  rst 0                      //1 reset as 48k if possible (15 bytes + 7)
.doSpectrum
  ld bc, 15                 //3
  ld hl, .spectrum          //3
  ld de, stack              //3
  push de                   //1
  ldir                      //2
  ret                       //1 (13 bytes + 22)
.setIntVec
  di                        //1
  ld (userInt), hl          //3
  ei                        //1
  retn                      //2 (7 bytes + 35)
  //should be 4 bytes spare
.userIntNoSys:
  ex (sp), hl
  pop hl                    // a botch to pop stack using no registers
  reti                      // exit interrupts without doing system things
  upto $66
.nmi66:
.warm:
  // warm boot code and NMI does it too
  ld bc, bankPort
  out (c), a
  ld hl, .userIntDefault  // a default user interrupt
  call setIntVec
  jr .exe
.start:
  //the start up code to initial once only values for cold boot
  jr .warm
.exe:
  //main loop as initialized now
  ret // unreachable??
.vector:// enter vector with hl set to the 3 byte vector address/bank switch
  ld (temphl), hl
  ld h, (lastBank)
  push hl         //save lastbank used
  push af
  ld a, (hl)
  push bc
  load bc, bankPort
  out (c), a
  ld (lastBank), a  //put new lastBank
  pop bc
  pop af          //restored
  ld hl, .onReturn  //local sub-label address
  push hl           //return there
  ld hl, (temphl)
  inc hl
  jpj hl            //indirect vector to return to onReturn
.onReturn:
  ld (temphl), hl
  pop hl
  push af
  ld a, h
  bush bc
  ld bc, bankPort
  out (c), a
  ld (lastBank), a    //restored lastBank
  pop bc
  pop af
  ld hl, (temphl)     //restore hl
  ret
.sysInt:
  // the system interrupt code fixed in ROM

  reti
.userIntDefault:
  ret
notice:
  ds "ZX FORTH ROM"




  // an interface I fix to prevent code intercept
  upto $1706
  jr ifaceIFix
  nop   // must not fetch $1708 as interface I intercepts on this address
ifaceIFix:


  upto $3d00          // $4000 - ($80 - $20) * 8
chars:
; $20 - Character: ' '          CHR$(32)

  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000

; $21 - Character: '!'          CHR$(33)

  db    %00000000
  db    %00010000
  db    %00010000
  db    %00010000
  db    %00010000
  db    %00000000
  db    %00010000
  db    %00000000

; $22 - Character: '"'          CHR$(34)

  db    %00000000
  db    %00100100
  db    %00100100
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000

; $23 - Character: '#'          CHR$(35)

  db    %00000000
  db    %00100100
  db    %01111110
  db    %00100100
  db    %00100100
  db    %01111110
  db    %00100100
  db    %00000000

; $24 - Character: '$'          CHR$(36)

  db    %00000000
  db    %00001000
  db    %00111110
  db    %00101000
  db    %00111110
  db    %00001010
  db    %00111110
  db    %00001000

; $25 - Character: '%'          CHR$(37)

  db    %00000000
  db    %01100010
  db    %01100100
  db    %00001000
  db    %00010000
  db    %00100110
  db    %01000110
  db    %00000000

; $26 - Character: '&'          CHR$(38)

  db    %00000000
  db    %00010000
  db    %00101000
  db    %00010000
  db    %00101010
  db    %01000100
  db    %00111010
  db    %00000000

; $27 - Character: '''          CHR$(39)

  db    %00000000
  db    %00001000
  db    %00010000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000

; $28 - Character: '('          CHR$(40)

  db    %00000000
  db    %00000100
  db    %00001000
  db    %00001000
  db    %00001000
  db    %00001000
  db    %00000100
  db    %00000000

; $29 - Character: ')'          CHR$(41)

  db    %00000000
  db    %00100000
  db    %00010000
  db    %00010000
  db    %00010000
  db    %00010000
  db    %00100000
  db    %00000000

; $2A - Character: '*'          CHR$(42)

  db    %00000000
  db    %00000000
  db    %00010100
  db    %00001000
  db    %00111110
  db    %00001000
  db    %00010100
  db    %00000000

; $2B - Character: '+'          CHR$(43)

  db    %00000000
  db    %00000000
  db    %00001000
  db    %00001000
  db    %00111110
  db    %00001000
  db    %00001000
  db    %00000000

; $2C - Character: ','          CHR$(44)

  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00001000
  db    %00001000
  db    %00010000

; $2D - Character: '-'          CHR$(45)

  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00111110
  db    %00000000
  db    %00000000
  db    %00000000

; $2E - Character: '.'          CHR$(46)

  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00011000
  db    %00011000
  db    %00000000

; $2F - Character: '/'          CHR$(47)

  db    %00000000
  db    %00000000
  db    %00000010
  db    %00000100
  db    %00001000
  db    %00010000
  db    %00100000
  db    %00000000

; $30 - Character: '0'          CHR$(48)

  db    %00000000
  db    %00111100
  db    %01000110
  db    %01001010
  db    %01010010
  db    %01100010
  db    %00111100
  db    %00000000

; $31 - Character: '1'          CHR$(49)

  db    %00000000
  db    %00011000
  db    %00101000
  db    %00001000
  db    %00001000
  db    %00001000
  db    %00111110
  db    %00000000

; $32 - Character: '2'          CHR$(50)

  db    %00000000
  db    %00111100
  db    %01000010
  db    %00000010
  db    %00111100
  db    %01000000
  db    %01111110
  db    %00000000

; $33 - Character: '3'          CHR$(51)

  db    %00000000
  db    %00111100
  db    %01000010
  db    %00001100
  db    %00000010
  db    %01000010
  db    %00111100
  db    %00000000

; $34 - Character: '4'          CHR$(52)

  db    %00000000
  db    %00001000
  db    %00011000
  db    %00101000
  db    %01001000
  db    %01111110
  db    %00001000
  db    %00000000

; $35 - Character: '5'          CHR$(53)

  db    %00000000
  db    %01111110
  db    %01000000
  db    %01111100
  db    %00000010
  db    %01000010
  db    %00111100
  db    %00000000

; $36 - Character: '6'          CHR$(54)

  db    %00000000
  db    %00111100
  db    %01000000
  db    %01111100
  db    %01000010
  db    %01000010
  db    %00111100
  db    %00000000

; $37 - Character: '7'          CHR$(55)

  db    %00000000
  db    %01111110
  db    %00000010
  db    %00000100
  db    %00001000
  db    %00010000
  db    %00010000
  db    %00000000

; $38 - Character: '8'          CHR$(56)

  db    %00000000
  db    %00111100
  db    %01000010
  db    %00111100
  db    %01000010
  db    %01000010
  db    %00111100
  db    %00000000

; $39 - Character: '9'          CHR$(57)

  db    %00000000
  db    %00111100
  db    %01000010
  db    %01000010
  db    %00111110
  db    %00000010
  db    %00111100
  db    %00000000

; $3A - Character: ':'          CHR$(58)

  db    %00000000
  db    %00000000
  db    %00000000
  db    %00010000
  db    %00000000
  db    %00000000
  db    %00010000
  db    %00000000

; $3B - Character: ';'          CHR$(59)

  db    %00000000
  db    %00000000
  db    %00010000
  db    %00000000
  db    %00000000
  db    %00010000
  db    %00010000
  db    %00100000

; $3C - Character: '<'          CHR$(60)

  db    %00000000
  db    %00000000
  db    %00000100
  db    %00001000
  db    %00010000
  db    %00001000
  db    %00000100
  db    %00000000

; $3D - Character: '='          CHR$(61)

  db    %00000000
  db    %00000000
  db    %00000000
  db    %00111110
  db    %00000000
  db    %00111110
  db    %00000000
  db    %00000000

; $3E - Character: '>'          CHR$(62)

  db    %00000000
  db    %00000000
  db    %00010000
  db    %00001000
  db    %00000100
  db    %00001000
  db    %00010000
  db    %00000000

; $3F - Character: '?'          CHR$(63)

  db    %00000000
  db    %00111100
  db    %01000010
  db    %00000100
  db    %00001000
  db    %00000000
  db    %00001000
  db    %00000000

; $40 - Character: '@'          CHR$(64)

  db    %00000000
  db    %00111100
  db    %01001010
  db    %01010110
  db    %01011110
  db    %01000000
  db    %00111100
  db    %00000000

; $41 - Character: 'A'          CHR$(65)

  db    %00000000
  db    %00111100
  db    %01000010
  db    %01000010
  db    %01111110
  db    %01000010
  db    %01000010
  db    %00000000

; $42 - Character: 'B'          CHR$(66)

  db    %00000000
  db    %01111100
  db    %01000010
  db    %01111100
  db    %01000010
  db    %01000010
  db    %01111100
  db    %00000000

; $43 - Character: 'C'          CHR$(67)

  db    %00000000
  db    %00111100
  db    %01000010
  db    %01000000
  db    %01000000
  db    %01000010
  db    %00111100
  db    %00000000

; $44 - Character: 'D'          CHR$(68)

  db    %00000000
  db    %01111000
  db    %01000100
  db    %01000010
  db    %01000010
  db    %01000100
  db    %01111000
  db    %00000000

; $45 - Character: 'E'          CHR$(69)

  db    %00000000
  db    %01111110
  db    %01000000
  db    %01111100
  db    %01000000
  db    %01000000
  db    %01111110
  db    %00000000

; $46 - Character: 'F'          CHR$(70)

  db    %00000000
  db    %01111110
  db    %01000000
  db    %01111100
  db    %01000000
  db    %01000000
  db    %01000000
  db    %00000000

; $47 - Character: 'G'          CHR$(71)

  db    %00000000
  db    %00111100
  db    %01000010
  db    %01000000
  db    %01001110
  db    %01000010
  db    %00111100
  db    %00000000

; $48 - Character: 'H'          CHR$(72)

  db    %00000000
  db    %01000010
  db    %01000010
  db    %01111110
  db    %01000010
  db    %01000010
  db    %01000010
  db    %00000000

; $49 - Character: 'I'          CHR$(73)

  db    %00000000
  db    %00111110
  db    %00001000
  db    %00001000
  db    %00001000
  db    %00001000
  db    %00111110
  db    %00000000

; $4A - Character: 'J'          CHR$(74)

  db    %00000000
  db    %00000010
  db    %00000010
  db    %00000010
  db    %01000010
  db    %01000010
  db    %00111100
  db    %00000000

; $4B - Character: 'K'          CHR$(75)

  db    %00000000
  db    %01000100
  db    %01001000
  db    %01110000
  db    %01001000
  db    %01000100
  db    %01000010
  db    %00000000

; $4C - Character: 'L'          CHR$(76)

  db    %00000000
  db    %01000000
  db    %01000000
  db    %01000000
  db    %01000000
  db    %01000000
  db    %01111110
  db    %00000000

; $4D - Character: 'M'          CHR$(77)

  db    %00000000
  db    %01000010
  db    %01100110
  db    %01011010
  db    %01000010
  db    %01000010
  db    %01000010
  db    %00000000

; $4E - Character: 'N'          CHR$(78)

  db    %00000000
  db    %01000010
  db    %01100010
  db    %01010010
  db    %01001010
  db    %01000110
  db    %01000010
  db    %00000000

; $4F - Character: 'O'          CHR$(79)

  db    %00000000
  db    %00111100
  db    %01000010
  db    %01000010
  db    %01000010
  db    %01000010
  db    %00111100
  db    %00000000

; $50 - Character: 'P'          CHR$(80)

  db    %00000000
  db    %01111100
  db    %01000010
  db    %01000010
  db    %01111100
  db    %01000000
  db    %01000000
  db    %00000000

; $51 - Character: 'Q'          CHR$(81)

  db    %00000000
  db    %00111100
  db    %01000010
  db    %01000010
  db    %01010010
  db    %01001010
  db    %00111100
  db    %00000000

; $52 - Character: 'R'          CHR$(82)

  db    %00000000
  db    %01111100
  db    %01000010
  db    %01000010
  db    %01111100
  db    %01000100
  db    %01000010
  db    %00000000

; $53 - Character: 'S'          CHR$(83)

  db    %00000000
  db    %00111100
  db    %01000000
  db    %00111100
  db    %00000010
  db    %01000010
  db    %00111100
  db    %00000000

; $54 - Character: 'T'          CHR$(84)

  db    %00000000
  db    %11111110
  db    %00010000
  db    %00010000
  db    %00010000
  db    %00010000
  db    %00010000
  db    %00000000

; $55 - Character: 'U'          CHR$(85)

  db    %00000000
  db    %01000010
  db    %01000010
  db    %01000010
  db    %01000010
  db    %01000010
  db    %00111100
  db    %00000000

; $56 - Character: 'V'          CHR$(86)

  db    %00000000
  db    %01000010
  db    %01000010
  db    %01000010
  db    %01000010
  db    %00100100
  db    %00011000
  db    %00000000

; $57 - Character: 'W'          CHR$(87)

  db    %00000000
  db    %01000010
  db    %01000010
  db    %01000010
  db    %01000010
  db    %01011010
  db    %00100100
  db    %00000000

; $58 - Character: 'X'          CHR$(88)

  db    %00000000
  db    %01000010
  db    %00100100
  db    %00011000
  db    %00011000
  db    %00100100
  db    %01000010
  db    %00000000

; $59 - Character: 'Y'          CHR$(89)

  db    %00000000
  db    %10000010
  db    %01000100
  db    %00101000
  db    %00010000
  db    %00010000
  db    %00010000
  db    %00000000

; $5A - Character: 'Z'          CHR$(90)

  db    %00000000
  db    %01111110
  db    %00000100
  db    %00001000
  db    %00010000
  db    %00100000
  db    %01111110
  db    %00000000

; $5B - Character: '['          CHR$(91)

  db    %00000000
  db    %00001110
  db    %00001000
  db    %00001000
  db    %00001000
  db    %00001000
  db    %00001110
  db    %00000000

; $5C - Character: '\'          CHR$(92)

  db    %00000000
  db    %00000000
  db    %01000000
  db    %00100000
  db    %00010000
  db    %00001000
  db    %00000100
  db    %00000000

; $5D - Character: ']'          CHR$(93)

  db    %00000000
  db    %01110000
  db    %00010000
  db    %00010000
  db    %00010000
  db    %00010000
  db    %01110000
  db    %00000000

; $5E - Character: '^'          CHR$(94)

  db    %00000000
  db    %00010000
  db    %00111000
  db    %01010100
  db    %00010000
  db    %00010000
  db    %00010000
  db    %00000000

; $5F - Character: '_'          CHR$(95)

  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %11111111

; $60 - Character: ' £ '        CHR$(96)

  db    %00000000
  db    %00011100
  db    %00100010
  db    %01111000
  db    %00100000
  db    %00100000
  db    %01111110
  db    %00000000

; $61 - Character: 'a'          CHR$(97)

  db    %00000000
  db    %00000000
  db    %00111000
  db    %00000100
  db    %00111100
  db    %01000100
  db    %00111100
  db    %00000000

; $62 - Character: 'b'          CHR$(98)

  db    %00000000
  db    %00100000
  db    %00100000
  db    %00111100
  db    %00100010
  db    %00100010
  db    %00111100
  db    %00000000

; $63 - Character: 'c'          CHR$(99)

  db    %00000000
  db    %00000000
  db    %00011100
  db    %00100000
  db    %00100000
  db    %00100000
  db    %00011100
  db    %00000000

; $64 - Character: 'd'          CHR$(100)

  db    %00000000
  db    %00000100
  db    %00000100
  db    %00111100
  db    %01000100
  db    %01000100
  db    %00111100
  db    %00000000

; $65 - Character: 'e'          CHR$(101)

  db    %00000000
  db    %00000000
  db    %00111000
  db    %01000100
  db    %01111000
  db    %01000000
  db    %00111100
  db    %00000000

; $66 - Character: 'f'          CHR$(102)

  db    %00000000
  db    %00001100
  db    %00010000
  db    %00011000
  db    %00010000
  db    %00010000
  db    %00010000
  db    %00000000

; $67 - Character: 'g'          CHR$(103)

  db    %00000000
  db    %00000000
  db    %00111100
  db    %01000100
  db    %01000100
  db    %00111100
  db    %00000100
  db    %00111000

; $68 - Character: 'h'          CHR$(104)

  db    %00000000
  db    %01000000
  db    %01000000
  db    %01111000
  db    %01000100
  db    %01000100
  db    %01000100
  db    %00000000

; $69 - Character: 'i'          CHR$(105)

  db    %00000000
  db    %00010000
  db    %00000000
  db    %00110000
  db    %00010000
  db    %00010000
  db    %00111000
  db    %00000000

; $6A - Character: 'j'          CHR$(106)

  db    %00000000
  db    %00000100
  db    %00000000
  db    %00000100
  db    %00000100
  db    %00000100
  db    %00100100
  db    %00011000

; $6B - Character: 'k'          CHR$(107)

  db    %00000000
  db    %00100000
  db    %00101000
  db    %00110000
  db    %00110000
  db    %00101000
  db    %00100100
  db    %00000000

; $6C - Character: 'l'          CHR$(108)

  db    %00000000
  db    %00010000
  db    %00010000
  db    %00010000
  db    %00010000
  db    %00010000
  db    %00001100
  db    %00000000

; $6D - Character: 'm'          CHR$(109)

  db    %00000000
  db    %00000000
  db    %01101000
  db    %01010100
  db    %01010100
  db    %01010100
  db    %01010100
  db    %00000000

; $6E - Character: 'n'          CHR$(110)

  db    %00000000
  db    %00000000
  db    %01111000
  db    %01000100
  db    %01000100
  db    %01000100
  db    %01000100
  db    %00000000

; $6F - Character: 'o'          CHR$(111)

  db    %00000000
  db    %00000000
  db    %00111000
  db    %01000100
  db    %01000100
  db    %01000100
  db    %00111000
  db    %00000000

; $70 - Character: 'p'          CHR$(112)

  db    %00000000
  db    %00000000
  db    %01111000
  db    %01000100
  db    %01000100
  db    %01111000
  db    %01000000
  db    %01000000

; $71 - Character: 'q'          CHR$(113)

  db    %00000000
  db    %00000000
  db    %00111100
  db    %01000100
  db    %01000100
  db    %00111100
  db    %00000100
  db    %00000110

; $72 - Character: 'r'          CHR$(114)

  db    %00000000
  db    %00000000
  db    %00011100
  db    %00100000
  db    %00100000
  db    %00100000
  db    %00100000
  db    %00000000

; $73 - Character: 's'          CHR$(115)

  db    %00000000
  db    %00000000
  db    %00111000
  db    %01000000
  db    %00111000
  db    %00000100
  db    %01111000
  db    %00000000

; $74 - Character: 't'          CHR$(116)

  db    %00000000
  db    %00010000
  db    %00111000
  db    %00010000
  db    %00010000
  db    %00010000
  db    %00001100
  db    %00000000

; $75 - Character: 'u'          CHR$(117)

  db    %00000000
  db    %00000000
  db    %01000100
  db    %01000100
  db    %01000100
  db    %01000100
  db    %00111000
  db    %00000000

; $76 - Character: 'v'          CHR$(118)

  db    %00000000
  db    %00000000
  db    %01000100
  db    %01000100
  db    %00101000
  db    %00101000
  db    %00010000
  db    %00000000

; $77 - Character: 'w'          CHR$(119)

  db    %00000000
  db    %00000000
  db    %01000100
  db    %01010100
  db    %01010100
  db    %01010100
  db    %00101000
  db    %00000000

; $78 - Character: 'x'          CHR$(120)

  db    %00000000
  db    %00000000
  db    %01000100
  db    %00101000
  db    %00010000
  db    %00101000
  db    %01000100
  db    %00000000

; $79 - Character: 'y'          CHR$(121)

  db    %00000000
  db    %00000000
  db    %01000100
  db    %01000100
  db    %01000100
  db    %00111100
  db    %00000100
  db    %00111000

; $7A - Character: 'z'          CHR$(122)

  db    %00000000
  db    %00000000
  db    %01111100
  db    %00001000
  db    %00010000
  db    %00100000
  db    %01111100
  db    %00000000

; $7B - Character: '{'          CHR$(123)

  db    %00000000
  db    %00001110
  db    %00001000
  db    %00110000
  db    %00001000
  db    %00001000
  db    %00001110
  db    %00000000

; $7C - Character: '|'          CHR$(124)

  db    %00000000
  db    %00001000
  db    %00001000
  db    %00001000
  db    %00001000
  db    %00001000
  db    %00001000
  db    %00000000

; $7D - Character: '}'          CHR$(125)

  db    %00000000
  db    %01110000
  db    %00010000
  db    %00001100
  db    %00010000
  db    %00010000
  db    %01110000
  db    %00000000

; $7E - Character: '~'          CHR$(126)

  db    %00000000
  db    %00010100
  db    %00101000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000
  db    %00000000

; $7F - Character: ' © '        CHR$(127)

  db    %00111100
  db    %01000010
  db    %10011001
  db    %10100001
  db    %10100001
  db    %10011001
  db    %01000010
  db    %00111100
  upto $4000    // end ROM
charUDG:
  fill 1280     // UDG space for (char+32) mod 255
  // stacks for forth
stack:
  fill $ff  //for 256 bytes
dstack:
  fill $ff  //for 256 bytes
  // system variables
temphl:
  dw 0
userInt:
  dw 0
lastBank:
  db 0

  // vectors if paging used
vectors:
  // paged vectored jumps
  db @start //@ for page and screen 1
  dw start
  //more vectors here

codeEnd:
  end
