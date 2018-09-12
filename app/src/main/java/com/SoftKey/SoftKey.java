package com.SoftKey;

import android.hardware.usb.UsbDevice;
import java.io.UnsupportedEncodingException;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import java.util.concurrent.Semaphore;

public class SoftKey {

    private static final int VID = 13961;
    private static final int PID = 34658;
    private static short MAX_LEN = 2031;
    private UsbManager mUsbManager;
    private Semaphore hsignal;
    private  byte g_ele1, g_ele2,  g_ele3, g_ele4;
    private byte [] Buffer;
    private byte [] EncBuffer;
    int lasterror;
    public SoftKey(UsbManager InUsbManager)
    {
        //mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        Buffer=new byte[2048];
        EncBuffer=new byte[8];
        mUsbManager=InUsbManager;
        hsignal=new Semaphore(1,true);
    }
    private byte [] HexStringToByteArray(String InString)
    {
        int nlen;
        int retutn_len;
        int n,i;
        byte [] b;
        String temp;
        nlen = InString.length();
        if (nlen < 16) retutn_len = 16;
        retutn_len = nlen / 2;
        b = new byte[retutn_len];
        i = 0;
        for(n=0;n<nlen;n=n+2)
        {
            temp = InString.substring( n, n+2);
            b[i] =(byte) HexToInt(temp);
            i = i + 1;
        }
        return b;
    }
    private String myhex(byte indata)
    {
        String outstring;
        outstring=String.format("%X",indata);
        if(outstring.length()<2)outstring="0"+outstring;
        return outstring;
    }
    private String AddZero(String InKey)
    {
        int nlen;
        int n;
        nlen =InKey.length();
        for(n=nlen;n<=7;n++)
        {
            InKey = "0" + InKey;
        }
        return  InKey;
    }


    private void myconvert(String HKey, String LKey, byte [] out_data)
    {
        String temp_String;
        HKey = AddZero(HKey);
        LKey = AddZero(LKey);
        int n;
        for(n=0;n<=3;n++)
        {
            temp_String=HKey.substring(  n * 2, (n * 2)+2);
            out_data[n] = (byte)HexToInt(temp_String);
        }
        for(n=0;n<=3;n++)
        {
            temp_String=LKey.substring( n * 2, (n * 2)+2);
            out_data[n + 4] = (byte)HexToInt(temp_String);
        }
    }

    //若某字节为负数则需将其转成无符号正数
    private  long conver(byte temp){
        long tempInt = (int)temp;
        if(tempInt < 0){
            tempInt += 256;
        }
        return tempInt;
    }

    //以下用于将16进制字符串转化为无符号长整型
    private int HexToInt(String s)
    {
        String [] hexch = { "0", "1", "2", "3", "4", "5", "6", "7",
                "8", "9", "A", "B", "C", "D", "E", "F"};
        int i, j;
        int r, n, k;
        String ch;

        k = 1; r = 0;
        for (i = s.length(); i > 0; i--)
        {
            ch = s.substring(i - 1,  i-1+1);
            n = 0;
            for (j = 0; j < 16; j++)
            {
                if (ch.compareToIgnoreCase(hexch[j]) ==0 )
                {
                    n = j;
                }
            }
            r += (n * k);
            k *= 16;
        }
        return r;
    }

    private UsbDeviceConnection OpenMydivece(UsbDevice hUsbDevice)
    {
        UsbDeviceConnection connection=null;
        if (hUsbDevice.getInterfaceCount() != 1) {
            lasterror=-72;
            return connection;
        }
        UsbInterface intf = hUsbDevice.getInterface(0);
        if (intf.getEndpointCount() != 1) {
            lasterror=-73;
            return connection;
        }
        connection = mUsbManager.openDevice(hUsbDevice);
        if (connection == null){lasterror = -89;return connection;}
        if ( connection.claimInterface(intf, true))
        {
            return connection;
        } else {
            lasterror=-90;
        }
        return connection;
    }

    private int F_GetVersionEx( byte VersionEx [],UsbDevice hUsbDevice)
    {
        byte[] array_in = new byte[25];
        byte[] array_out = new byte[25];
        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 5;
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        if (mConnection.controlTransfer(0xa1, 0x1, 0x0301, 0, array_out,0x15, 0)<0){mConnection.close();lasterror=-93; return -93;}
        mConnection.close();
        VersionEx[0] = array_out[0];
        return 0;
    }

    private int sub_YRead( int address, byte [] password, UsbDevice hUsbDevice)
    {
        int n;
        byte []array_in=new byte[25];
        byte []array_out=new byte[25];
        byte opcode;
        if ((address > 495) || (address < 0)){lasterror=-81;return lasterror;}
        opcode = (byte)128;
        if (address > 255)
        {
            opcode = (byte)160;
            address = address - 256;
        }

        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 16;
        array_in[2] = opcode;
        array_in[3] = (byte)address;
        array_in[4] = (byte)address;
        for(n=0;n<8;n++)
        {
            array_in[5 + n] = password[n];
        }
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        if (mConnection.controlTransfer(0xa1, 0x1, 0x0301, 0, array_out,0x15, 0)<0){mConnection.close();lasterror=-93; return -93;}
        mConnection.close();
        if (array_out[0] != 83)
        {
            lasterror=-83;return 0;
        }
        return array_out[1];
    }

    private int sub_YWrite(byte indata, int address, byte [] password, UsbDevice hUsbDevice)
    {
        int n;
        byte []array_in=new byte[25];
        byte []array_out=new byte[25];
        byte opcode ;
        if ((address > 511) || (address < 0)) return -81;
        opcode = 64;
        if (address > 255)
        {
            opcode = 96;
            address = address - 256;
        }

        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 17;
        array_in[2] = opcode;
        array_in[3] = (byte)address;
        array_in[4] = indata;
        for(n=0;n<8;n++)
        {
            array_in[5 + n] = password[n];
        }
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        if (mConnection.controlTransfer(0xa1, 0x1, 0x0301, 0, array_out,0x15, 0)<0){mConnection.close();lasterror=-93; return -93;}
        mConnection.close();
        if (array_out[1] != 1)
        {
            lasterror=-82;return -82;
        }
        return 0;
    }


    private int NT_ReSet(UsbDevice hUsbDevice)
    {
        byte[] array_in = new byte[25];
        byte[] array_out = new byte[25];
        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 32;
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        if (mConnection.controlTransfer(0xa1, 0x1, 0x0301, 0, array_out,0x15, 0)<0){mConnection.close();lasterror=-93; return -93;}
        mConnection.close();
        if (array_out[0] != 0)
        {
            lasterror=-82;return -lasterror;
        }
        return 0;
    }
    private int NT_SetID(byte[] InBuf,UsbDevice hUsbDevice)
    {
        int n;
        byte[] array_in = new byte[25];
        byte[] array_out = new byte[25];
        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 7;
        for (n = 2; n <= 9; n++)
        {
            array_in[n] = InBuf[n - 2 ];
        }
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        if (mConnection.controlTransfer(0xa1, 0x1, 0x0301, 0, array_out,0x15, 0)<0){mConnection.close();lasterror=-93; return -93;}
        mConnection.close();
        if (array_out[0] != 0x0)
        {
            return -82;
        }
        return 0;
    }

    private int Y_Read(byte [] OutData, int address , int nlen , byte [] password ,UsbDevice hUsbDevice , int pos)
    {
        int addr_l;
        int addr_h ;
        int n;
        byte []array_in=new byte[25];
        byte []array_out=new byte[25];
        lasterror=0;
        if ((address > MAX_LEN) || (address < 0)) return -81;
        if ((nlen > 16)) return -87;
        if ((nlen + address) > MAX_LEN) return -88;
        addr_h = (address >>> 8) * 2;
        addr_l = address & 255;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}

        array_in[1] = 0x12;
        array_in[2] = (byte)addr_h;
        array_in[3] = (byte)addr_l;
        array_in[4] = (byte)nlen;
        for(n=0;n<=7;n++)
        {
            array_in[5 + n] = password[n];
        }
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        if (mConnection.controlTransfer(0xa1, 0x1, 0x0301, 0, array_out,0x15, 0)<0){mConnection.close();lasterror=-93; return -93;}
        mConnection.close();
        if (array_out[0] != 0)
        {
            return -83;
        }
        for(n=0;n<nlen;n++)
        {
            OutData[n + pos] = array_out[n + 1];
        }
        return 0;
    }

    private int Y_Write(byte [] indata, int address, int nlen, byte [] password, UsbDevice hUsbDevice, int pos)
    {
        int addr_l;
        int addr_h ;
        int n;
        byte []array_in=new byte[25];
        byte []array_out=new byte[25];
        lasterror=0;
        if ((nlen > 8)) return -87;
        if ((address + nlen - 1) > (MAX_LEN + 17) || (address < 0)) return -81;
        addr_h = (address >>> 8) * 2;
        addr_l = address & 255;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 0x13;
        array_in[2] = (byte)addr_h;
        array_in[3] = (byte)addr_l;
        array_in[4] = (byte)nlen;
        for(n=0;n<=7;n++)
        {
            array_in[5 + n] = password[n];
        }
        for(n=0;n<nlen;n++)
        {
            array_in[13 + n] = indata[n + pos];
        }
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        if (mConnection.controlTransfer(0xa1, 0x1, 0x0301, 0, array_out,0x15, 0)<0){mConnection.close();lasterror=-93; return -93;}
        mConnection.close();
        if (array_out[0] != 0)
        {
            return -82;
        }
        return 0;
    }

    private int NT_Cal(byte [] InBuf , byte [] outbuf, UsbDevice hUsbDevice, int pos)
    {
        int n;
        byte []array_in=new byte[25];
        byte []array_out=new byte[25];
        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 8;
        for(n=2;n<=9;n++)
        {
            array_in[n] = InBuf[n - 2 + pos];
        }
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        if (mConnection.controlTransfer(0xa1, 0x1, 0x0301, 0, array_out,0x15, 0)<0){mConnection.close();lasterror=-93; return -93;}
        mConnection.close();
        for(n=0;n <8;n++)
        {
            outbuf[n + pos] = array_out[n];
        }
        if( array_out[8] != 0x55)
        {
            return -20;
        }
        return 0;
    }

    private int NT_SetCal_2(byte [] indata, byte IsHi, UsbDevice hUsbDevice, int pos)
    {

        int n;
        byte []array_in=new byte[25];
        byte []array_out=new byte[25];
        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 9;
        array_in[2] = IsHi;
        for(n=0;n <8;n++)
        {
            array_in[3 + n] = indata[n + pos];
        }
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        if (mConnection.controlTransfer(0xa1, 0x1, 0x0301, 0, array_out,0x15, 0)<0){mConnection.close();lasterror=-93; return -93;}
        mConnection.close();
        if (array_out[0] != 0)
        {
            return -82;
        }

        return 0;
    }
    private int NT_Cal_New(byte[] InBuf, byte[] outbuf, UsbDevice hUsbDevice, int pos)
    {
        int n;
        byte[] array_in = new byte[25];
        byte[] array_out = new byte[25];
        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 12;
        for (n = 2; n <= 9; n++)
        {
            array_in[n] = InBuf[n - 2 + pos];
        }
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        if (mConnection.controlTransfer(0xa1, 0x1, 0x0301, 0, array_out,0x15, 0)<0){mConnection.close();lasterror=-93; return -93;}
        mConnection.close();
        for (n = 0; n < 8; n++)
        {
            outbuf[n + pos] = array_out[n];
        }
        if (array_out[8] != 0x55)
        {
            return -20;
        }
        return 0;
    }

    private int NT_SetCal_New(byte[] indata, byte IsHi,UsbDevice hUsbDevice, int pos)
    {

        int n;
        byte[] array_in = new byte[25];
        byte[] array_out = new byte[25];
        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 13;
        array_in[2] = IsHi;
        for (n = 0; n < 8; n++)
        {
            array_in[3 + n] = indata[n + pos];
        }
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        if (mConnection.controlTransfer(0xa1, 0x1, 0x0301, 0, array_out,0x15, 0)<0){mConnection.close();lasterror=-93; return -93;}
        mConnection.close();
        if (array_out[0] != 0)
        {
            return -82;
        }

        return 0;
    }

    private int NT_GetID_1(UsbDevice hUsbDevice)
    {
        int [] t=new int[8];
        byte []array_in=new byte[25];
        byte []array_out=new byte[25];
        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 2;
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        if (mConnection.controlTransfer(0xa1, 0x1, 0x0301, 0, array_out,0x15, 0)<0){mConnection.close();lasterror=-93; return -93;}
        mConnection.close();
        t[0] = (int)conver(array_out[0]) ; t[1] = (int)conver(array_out[1]) ; t[2] = (int)conver(array_out[2]) ; t[3] = (int)conver(array_out[3]);
        t[4] = (int)conver(array_out[4]) ; t[5] = (int)conver(array_out[5]) ; t[6] = (int)conver(array_out[6]) ; t[7] = (int)conver(array_out[7]);
        return ( t[3] | (t[2] << 8) | (t[1] << 16) | (t[0] << 24));
    }

    private int NT_GetID_2(UsbDevice hUsbDevice)
    {
        int [] t=new int[8];
        byte []array_in=new byte[25];
        byte []array_out=new byte[25];
        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 2;
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        if (mConnection.controlTransfer(0xa1, 0x1, 0x0301, 0, array_out,0x15, 0)<0){mConnection.close();lasterror=-93; return -93;}
        mConnection.close();
        t[0] = (int)conver(array_out[0]) ; t[1] = (int)conver(array_out[1]) ; t[2] = (int)conver(array_out[2]) ; t[3] = (int)conver(array_out[3]);
        t[4] = (int)conver(array_out[4]) ; t[5] = (int)conver(array_out[5]) ; t[6] = (int)conver(array_out[6]) ; t[7] = (int)conver(array_out[7]);
        return (t[7] | (t[6] << 8) | (t[5] << 16) | (t[4] << 24));
    }

    private int NT_GetVersion(byte Version [], UsbDevice hUsbDevice)
    {
        byte []array_in=new byte[25];
        byte []array_out=new byte[25];
        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 1;
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        if (mConnection.controlTransfer(0xa1, 0x1, 0x0301, 0, array_out,0x15, 0)<0){mConnection.close(); lasterror=-93; return -93;}
        mConnection.close();
        Version[0] = array_out[0];
        return 0;
    }

    private int NT_Read(UsbDevice hUsbDevice)
    {
        byte []array_out=new byte[25];
        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        if (mConnection.controlTransfer(0xa1, 0x1, 0x0301, 0, array_out,0x15, 0)<0){mConnection.close(); lasterror=-93; return -93;}
        mConnection.close();
        g_ele1 = array_out[0];
        g_ele2 = array_out[1];
        g_ele3 = array_out[2];
        g_ele4 = array_out[3];
        return 0;
    }

    private int NT_Write(byte ele1, byte ele2, byte ele3, byte ele4, UsbDevice hUsbDevice)
    {
        byte []array_in=new byte[25];
        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 3 ; array_in[2] = ele1 ; array_in[3] = ele2 ; array_in[4] = ele3 ; array_in[5] = ele4;
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        mConnection.close();
        return 0;
    }

    private int NT_Write_2(byte ele1, byte ele2, byte ele3, byte ele4, UsbDevice hUsbDevice)
    {
        byte []array_in=new byte[25];
        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 4 ; array_in[2] = ele1 ; array_in[3] = ele2 ; array_in[4] = ele3 ; array_in[5] = ele4;
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        mConnection.close();
        return 0;
    }

    private int NT_Write_New(byte ele1, byte ele2, byte ele3, byte ele4, UsbDevice hUsbDevice)
    {
        byte[] array_in = new byte[25];
        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 0x0a; array_in[2] = ele1; array_in[3] = ele2; array_in[4] = ele3; array_in[5] = ele4;
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        mConnection.close();
        return 0;
    }

    private int NT_Write_2_New(byte ele1, byte ele2, byte ele3, byte ele4, UsbDevice hUsbDevice)
    {
        byte[] array_in = new byte[25];
        lasterror=0;
        UsbDeviceConnection mConnection=OpenMydivece( hUsbDevice);
        if(lasterror!=0){return lasterror;}
        array_in[1] = 0x0b; array_in[2] = ele1; array_in[3] = ele2; array_in[4] = ele3; array_in[5] = ele4;
        if (mConnection.controlTransfer(0x21, 0x9, 0x0302, 0, array_in,0x15, 0)<0){mConnection.close(); lasterror=-94;return -94;}
        mConnection.close();
        return 0;
    }

    private int ReadDword(UsbDevice Path)
    {
        int out_data;
        int t1;
        int t2;
        int t3;
        int t4;
        lasterror = NT_Read(Path);
        t1 = (int)conver(g_ele1) ; t2 = (int)conver(g_ele2) ; t3 = (int)conver(g_ele3) ; t4 = (int)conver(g_ele4);
        out_data = t1 | (t2 << 8) | (t3 << 16) | (t4 << 24);
        return out_data;
    }

    private int WriteDword_New(int in_data, UsbDevice Path)
    {
        byte b1;
        byte b2;
        byte b3;
        byte b4;
        b1 = (byte)(in_data & 255);
        b2 = (byte)((in_data >>> 8) & 255);
        b3 = (byte)((in_data >>> 16) & 255);
        b4 = (byte)((in_data >>> 24) & 255);
        return NT_Write_New(b1, b2, b3, b4, Path);
    }

    private int WriteDword_2_New(int in_data, UsbDevice Path)
    {
        byte b1;
        byte b2;
        byte b3;
        byte b4;
        b1 = (byte)(in_data & 255);
        b2 = (byte)((in_data >>> 8) & 255);
        b3 = (byte)((in_data >>> 16) & 255);
        b4 = (byte)((in_data >>> 24) & 255);
        return NT_Write_2_New(b1, b2, b3, b4, Path);
    }
    private int WriteDword(int in_data, UsbDevice Path)
    {
        byte b1;
        byte b2;
        byte b3;
        byte b4 ;
        b1 = (byte)( in_data & 255);
        b2 = (byte)((in_data >>> 8) & 255);
        b3 = (byte)((in_data >>> 16) & 255);
        b4 = (byte)((in_data >>> 24) & 255);
        return NT_Write(b1, b2, b3, b4, Path);
    }

    private int WriteDword_2(int in_data , UsbDevice Path)
    {
        byte b1;
        byte b2;
        byte b3;
        byte b4;
        b1 = (byte)(in_data & 255);
        b2 = (byte)((in_data >>> 8) & 255);
        b3 = (byte)((in_data >>> 16) & 255);
        b4 = (byte)((in_data >>> 24) & 255);
        return NT_Write_2(b1, b2, b3, b4, Path);
    }

    //获到锁的版本
    public  int GetVersion(UsbDevice InPath)
    {
        byte [] Version=new byte[1];
        try
        {
            hsignal.acquire();
            lasterror = NT_GetVersion(Version, InPath);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Version[0];
    }

    //获到锁的扩展版本
    public  int GetVersionEx(UsbDevice InPath)
    {
        byte [] VersionEx=new byte[1];
        try
        {
            hsignal.acquire();
            lasterror = F_GetVersionEx(VersionEx, InPath);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return VersionEx[0];
    }

    //获到锁的ID的前4个字节
    public  int GetID_1(UsbDevice InPath)
    {
        int id_1=0;
        try
        {
            hsignal.acquire();
            id_1 = NT_GetID_1(InPath);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return id_1;
    }

    //获到锁的ID的后4个字节
    public  int GetID_2(UsbDevice InPath)
    {
        int id_2=0;
        try
        {
            hsignal.acquire();
            id_2 = NT_GetID_2(InPath);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return id_2;
    }
    //获到锁的扩展版本
    public int NT_GetVersionEx(UsbDevice Path)
    {
        byte [] Version=new byte[1];
        try
        {
            hsignal.acquire();
            lasterror = F_GetVersionEx(Version, Path);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Version[0];
    }
    //返回最后的错误信息
    public  int get_LastError()
    {
        return lasterror;
    }
    //查找加密锁
    public  UsbDevice FindPort(int start)
    {
        int count=0;
        lasterror=0;
        for (UsbDevice device :  mUsbManager.getDeviceList().values())
        {
            if (device != null && device.getVendorId()==VID && device.getProductId()== PID)
            {
                if(count==start)
                {
                    return  device;
                }
                count++;
            }
        }
        lasterror=-92;
        return null;

    }

    //查找指定的加密锁
    public  UsbDevice FindPort_2(int start,int in_data,int verf_data)
    {
        int count=0;
        int pos;
        int out_data=0;
        UsbDevice OutPath=null;
        for(pos=0;pos<256;pos++)
        {
            OutPath=FindPort(pos);
            if (lasterror != 0 ) return OutPath;
            lasterror = WriteDword(in_data, OutPath);
            if (lasterror != 0 ) return OutPath;
            out_data = ReadDword( OutPath);
            if (lasterror != 0 ) return OutPath;
            if (out_data == verf_data  )
            {
                if(count==start)
                {
                    return OutPath;
                }
                else
                {
                    count++;
                }
            }
        }
        lasterror=-92;
        return null;
    }
    //查找指定的加密锁(使用普通算法二)
    public  UsbDevice FindPort_3(int start, long in_data, long verf_data)
    {
        int count=0;
        int pos;
        int out_data=0;
        UsbDevice OutPath=null;
        for(pos=0;pos<256;pos++)
        {
            OutPath=FindPort(pos);
            if (lasterror != 0 ) return OutPath;
            lasterror = WriteDword_New((int)in_data, OutPath);
            if (lasterror != 0 ) return OutPath;
            out_data = ReadDword( OutPath);
            if (lasterror != 0 ) return OutPath;
            if (out_data == verf_data  )
            {
                if(count==start)
                {
                    return OutPath;
                }
                else
                {
                    count++;
                }
            }
        }
        lasterror=-92;
        return null;
    }
    //设置读密码
    public  int SetReadPassword(String W_hkey, String W_lkey, String new_hkey, String new_lkey, UsbDevice InPath)
    {
        int ret=0;
        byte [] ary1=new byte[8];
        byte [] ary2=new byte[8];
        short address;
        myconvert(W_hkey, W_lkey, ary1);
        myconvert(new_hkey, new_lkey, ary2);
        address = 496;
        try
        {
            hsignal.acquire();
            ret = Y_Write(ary2, address, 8, ary1, InPath, 0);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
    }
    //设置写密码
    public  int SetWritePassword(String W_hkey, String W_lkey, String new_hkey, String new_lkey, UsbDevice InPath)
    {
        int ret=0;
        byte [] ary1=new byte[8];
        byte [] ary2=new byte[8];
        short address;
        myconvert(W_hkey, W_lkey, ary1);
        myconvert(new_hkey, new_lkey, ary2);
        address = 504;
        try
        {
            hsignal.acquire();
            ret = Y_Write(ary2, address, 8, ary1, InPath, 0);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
    }
    //普通算法函数
    public  int sWriteEx_New(int in_data, UsbDevice KeyPath)
    {
        int out_data=0;
        try
        {
            hsignal.acquire();
            lasterror = WriteDword_New(in_data, KeyPath);
            if (lasterror != 0) {hsignal.release();return lasterror;}
            out_data = ReadDword( KeyPath);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return out_data;
    }
    public  int sWrite_2Ex_New(int in_data, UsbDevice KeyPath)
    {
        int out_data=0;
        try
        {
            hsignal.acquire();
            lasterror = WriteDword_2_New(in_data, KeyPath);
            if (lasterror != 0) {hsignal.release();return lasterror;}
            out_data = ReadDword( KeyPath);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return out_data;
    }
    public  int sWriteEx(int InData, UsbDevice InPath)
    {
        int out_data=0;
        try
        {
            hsignal.acquire();
            lasterror = WriteDword(InData, InPath);
            if (lasterror != 0) {hsignal.release();return lasterror;}
            out_data = ReadDword( InPath);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return out_data;
    }
    public  int sWrite_2Ex(int InData, UsbDevice InPath)
    {
        int out_data=0;
        try
        {
            hsignal.acquire();
            lasterror = WriteDword_2(InData, InPath);
            if (lasterror != 0) {hsignal.release();return lasterror;}
            out_data = ReadDword( InPath);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return out_data;
    }
    public  int sRead(UsbDevice InPath)
    {
        int out_data=0;
        try
        {
            hsignal.acquire();
            out_data = ReadDword(InPath);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return out_data;
    }
    public  int sWrite(int InData,UsbDevice InPath)
    {
        int ret=0;
        try
        {
            hsignal.acquire();
            ret = WriteDword(InData, InPath);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
    }
    public  int sWrite_2(int InData,UsbDevice InPath)
    {
        int ret=0;
        try
        {
            hsignal.acquire();
            ret = WriteDword_2(InData, InPath);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
    }
    //从加密锁中读取一批字节
    public  int YReadEx(short Address,short len, String HKey, String LKey,UsbDevice InPath)
    {
        int ret=0;
        byte [] password=new byte[8];
        int n;
        int nlen=len;
        int address=Address;
        if ((address + nlen - 1 > MAX_LEN) || (address < 0)){lasterror=-81;return (-81);}
        myconvert(HKey, LKey, password);
        try
        {
            hsignal.acquire();
            for(n=0;n<nlen/16;n++)
            {
                ret = Y_Read(Buffer, address + n * 16, 16, password, InPath, n * 16);
                if (ret != 0){ hsignal.release();  return ret;}
            }
            if (nlen - 16 * n > 0)
            {
                ret = Y_Read(Buffer, address + n * 16, nlen - 16 * n, password, InPath, 16 * n);
                if (ret != 0){ hsignal.release();  return ret;}
            }
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
    }

    //从加密锁中读取一个字节，一般不使用
    public int YRead(short Address, String HKey, String LKey, UsbDevice InPath)
    {
        int ret=0;
        byte [] password=new byte[8];
        int address=Address;
        if ((address  > 495) || (address < 0)){lasterror=-81;return (-81);}
        myconvert(HKey, LKey, password);
        try
        {
            hsignal.acquire();
            ret = sub_YRead( address, password, InPath);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
    }


    //从缓冲区中获得数据
    public  short GetBuf(int pos)
    {
        if(pos>MAX_LEN || pos<0){lasterror=-198;return 0;}
        return Buffer[pos];
    }
    //写一批字节到加密锁中
    public  int YWriteEx(short Address, short len,String HKey, String LKey, UsbDevice InPath)
    {
        int ret=0;
        int nlen=len;
        int address=Address;
        byte [] password=new byte[8];
        int n;
        int leave;
        int temp_leave ;
        if ((address + nlen - 1 > MAX_LEN) || (address < 0)){lasterror=-81;return (-81);}
        myconvert(HKey, LKey, password);
        try
        {
            hsignal.acquire();
            temp_leave = address % 16;
            leave = 16 - temp_leave;
            if (leave > nlen) leave = nlen;
            if (leave > 0)
            {
                for(n=0;n<leave / 8;n++)
                {
                    ret = Y_Write(Buffer, address + n * 8, 8, password, InPath, 8 * n);
                    if (ret != 0){ hsignal.release(); return ret;}
                }
                if (leave - 8 * n > 0)
                {
                    ret = Y_Write(Buffer, address + n * 8, leave - n * 8, password, InPath, 8 * n);
                    if (ret != 0){ hsignal.release();return ret;}
                }
            }
            nlen = nlen - leave ; address = address + leave;
            if (nlen > 0)
            {

                for(n=0;n<nlen/8;n++)
                {
                    ret = Y_Write(Buffer, address + n * 8, 8, password, InPath, leave + 8 * n);
                    if (ret != 0){ hsignal.release(); return ret;}
                }
                if (nlen - 8 * n > 0)
                {
                    ret = Y_Write(Buffer, address + n * 8, nlen - n * 8, password, InPath, leave + 8 * n);
                    if (ret != 0){ hsignal.release(); return ret;}
                }
            }
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
    }
    //写一个字节到加密锁中，一般不使用
    public  int YWrite(short inData, short Address, String HKey, String LKey, UsbDevice InPath)
    {
        int ret=0;
        int address=Address;
        byte [] password=new byte[8];
        if ((address  > 495) || (address < 0)){lasterror=-81;return (-81);}
        myconvert(HKey, LKey, password);
        try
        {
            hsignal.acquire();

            lasterror = sub_YWrite((byte)inData, address , password, InPath);

            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
    }


    //设置要写入的缓冲区的数据
    public  int SetBuf(int pos,short Data)
    {
        if(pos>MAX_LEN || pos<0){lasterror= -199;return lasterror;}
        Buffer[pos]=(byte)Data;
        return 0;
    }
    //从加密锁中读字符串-新
    public  String NewReadString(int Address, int len, String HKey, String LKey,UsbDevice InPath)
    {
        return YReadString((short)Address,(short)len,HKey,LKey,InPath);
    }
    //写字符串到加密锁中-新
    public  int NewWriteString(String InString, int Address, String HKey, String LKey,UsbDevice InPath)
    {
        return YWriteString(InString,(short)Address,HKey,LKey,InPath);
    }
    //兼容旧的读写字符串函数，不再使用
    public  String YReadString(short Address, short len, String HKey, String LKey,UsbDevice InPath)
    {
        int nlen=len;
        int address=Address;
        byte [] ary1=new byte[8];
        int n;
        int total_len ;
        byte [] outb;
        outb=new byte[nlen];
        myconvert(HKey, LKey, ary1);
        if (address < 0) {lasterror= -81;return null;}
        total_len = address + nlen;
        if (total_len > MAX_LEN) {lasterror=  -47;return null;}
        try
        {
            hsignal.acquire();
            for(n=0;n<(nlen /16);n++)
            {
                lasterror = Y_Read(outb, address + n * 16, 16, ary1, InPath, n * 16);
                if (lasterror != 0){hsignal.release(); return null;}
            }
            if (nlen - 16 * n > 0)
            {
                lasterror = Y_Read(outb, address + n * 16, nlen - 16 * n, ary1, InPath, 16 * n);
                if (lasterror != 0){ hsignal.release(); return null;}
            }
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        try
        {
            return new String(outb,"gb2312");
        }
        catch (UnsupportedEncodingException e) {
            lasterror=-1012;
            return null;
        }
    }
    public  int YWriteString(String InString, short Address, String HKey, String LKey,UsbDevice InPath)
    {
        byte [] ary1=new byte[8];
        int n;
        int outlen ,temp_outlen;
        int total_len;
        int temp_leave ;
        int leave;
        byte [] b;
        int address=Address;
        if ((address < 0)) {lasterror=-81;return lasterror;}
        myconvert(HKey, LKey, ary1);
        try
        {
            b=InString.getBytes("gb2312");
            outlen= b.length;
            temp_outlen=outlen;
        }
        catch (UnsupportedEncodingException e) {
            lasterror=-1012;
            return lasterror;
        }

        total_len = address + outlen;
        if (total_len > MAX_LEN){lasterror=-47;return lasterror;}
        try
        {
            hsignal.acquire();
            temp_leave = address % 16;
            leave = 16 - temp_leave;
            if (leave > outlen)leave = outlen;

            if (leave > 0)
            {
                for(n=0;n<(leave / 8);n++)
                {
                    lasterror = Y_Write(b, address + n * 8, 8, ary1, InPath, n * 8);
                    if (lasterror != 0){ hsignal.release(); return lasterror;}
                }
                if (leave - 8 * n > 0)
                {
                    lasterror = Y_Write(b, address + n * 8, leave - n * 8, ary1, InPath, 8 * n);
                    if (lasterror != 0){hsignal.release(); return lasterror;}
                }
            }
            outlen = outlen - leave;
            address = address + leave;
            if (outlen > 0)
            {
                for(n=0;n<(outlen / 8);n++)
                {
                    lasterror = Y_Write(b, address + n * 8, 8, ary1, InPath, leave + n * 8);
                    if (lasterror != 0){ hsignal.release();return lasterror;}
                }
                if (outlen - 8 * n > 0)
                {
                    lasterror = Y_Write(b, address + n * 8, outlen - n * 8, ary1, InPath, leave + 8 * n);
                    if (lasterror != 0){ hsignal.release(); return lasterror;}
                }
            }
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp_outlen;
    }
    //'设置增强算法密钥一
    public  int SetCal_2(String Key, UsbDevice InPath)
    {
        byte [] KeyBuf= HexStringToByteArray(Key);//new byte[16];

        try
        {
            hsignal.acquire();
            lasterror = NT_SetCal_2(KeyBuf, (byte)0, InPath, 8);
            if (lasterror != 0) return lasterror;
            lasterror = NT_SetCal_2(KeyBuf, (byte)1, InPath, 0);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return lasterror;
    }
    //使用增强算法一对字符串进行加密
    public  String EncString(String InString, UsbDevice InPath)
    {
        byte [] b;
        byte [] outb;
        int n;
        int nlen;

        try
        {
            b=InString.getBytes("gb2312");
            nlen= b.length+1;
        }
        catch (UnsupportedEncodingException e) {
            lasterror=-1012;
            return null;
        }
        if( nlen < 8)
        {
            nlen = 8;
        }

        outb=new byte[nlen];

        System.arraycopy(b,0,outb,0,b.length);

        b=new byte[nlen];
        System.arraycopy(outb,0,b,0,outb.length);


        try
        {
            hsignal.acquire();
            for(n=0;n<=(nlen-8);n=n+8)
            {
                lasterror = NT_Cal(b, outb, InPath, n);
                if (lasterror != 0){ hsignal.release(); return null;}
            }
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        String outstring = "";
        for (n = 0 ;n<= nlen - 1;n++)
        {
            outstring = outstring +myhex(outb[n]) ;
        }
        return outstring;

    }
    //使用增强算法一对二进制数据进行加密
    public  int Cal(UsbDevice InPath)
    {
        try
        {
            hsignal.acquire();
            lasterror = NT_Cal(EncBuffer, EncBuffer, InPath, 0);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return lasterror;
    }
    //'设置增强算法密钥二
    public  int SetCal_New(String Key, UsbDevice InPath)
    {
        byte [] KeyBuf= HexStringToByteArray(Key);//new byte[16];
        try
        {
            hsignal.acquire();
            lasterror = NT_SetCal_New(KeyBuf, (byte)0, InPath, 8);
            if (lasterror != 0) return lasterror;
            lasterror = NT_SetCal_New(KeyBuf, (byte)1, InPath, 0);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return lasterror;
    }
    //使用增强算法二对字符串进行加密
    public  String EncString_New(String InString, UsbDevice InPath)
    {
        byte [] b;
        byte [] outb;
        int n;
        int nlen;

        try
        {
            b=InString.getBytes("gb2312");
            nlen= b.length+1;
        }
        catch (UnsupportedEncodingException e) {
            lasterror=-1012;
            return null;
        }
        if( nlen < 8)
        {
            nlen = 8;
        }

        outb=new byte[nlen];

        System.arraycopy(b,0,outb,0,b.length);

        b=new byte[nlen];
        System.arraycopy(outb,0,b,0,outb.length);


        try
        {
            hsignal.acquire();
            for(n=0;n<=(nlen-8);n=n+8)
            {
                lasterror = NT_Cal_New(b, outb, InPath, n);
                if (lasterror != 0){ hsignal.release(); return null;}
            }
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        String outstring = "";
        for (n = 0 ;n<= nlen - 1;n++)
        {
            outstring = outstring +myhex(outb[n]) ;
        }
        return outstring;


    }
    //使用增强算法二对二进制数据进行加密
    public  int Cal_New(UsbDevice InPath)
    {
        try
        {
            hsignal.acquire();
            lasterror = NT_Cal_New(EncBuffer, EncBuffer, InPath, 0);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return lasterror;
    }
    //使用增强算法对字符串进行解密，用软件的方式
    public  String DecString(String InString, String Key)
    {
        return StrEnc(InString,Key);
    }
    //设置要加密的缓冲区的数据
    public  int SetEncBuf(int pos, short Data)
    {
        if(pos>7 || pos<0){lasterror=-199;return lasterror;}
        EncBuffer[pos]=(byte)Data;
        return 0;

    }
    //从缓冲区中获取加密后的数据
    public  short GetEncBuf(int pos)
    {
        if(pos>7 || pos<0){lasterror=-198;return 0;}
        return EncBuffer[pos];
    }

    public int ReSet(UsbDevice Path)
    {
        try
        {
            hsignal.acquire();
            lasterror = NT_ReSet(Path);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return lasterror;
    }

    public int SetID(String Seed, UsbDevice Path)
    {
        int n,i;
        byte[] KeyBuf = new byte[8];
        int nlen;
        String temp;
        nlen = Seed.length();
        i = 0;
        for(n=0;n<nlen;n=n+2)
        {
            temp = Seed.substring( n, n+2);
            KeyBuf[i] =(byte) HexToInt(temp);
            i = i + 1;
        }

        try
        {
            hsignal.acquire();
            lasterror = NT_SetID(KeyBuf,  Path);
            hsignal.release();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return lasterror;
    }

    //使用增强算法，加密字符串，用软件的方式
    public String  StrEnc(String InString , String Key)
    {

        byte [] b,outb;
        byte []temp_b;
        byte [] temp=new byte[8],outtemp=new byte[8];
        try
        {
            temp_b=InString.getBytes("gb2312");
        }
        catch (UnsupportedEncodingException e) {
            lasterror=-1012;
            return null;
        }
        int n,i,nlen,outlen;
        String outstring;

        nlen = temp_b.length;
        nlen=nlen+1;
        if( nlen < 8 )
            outlen = 8;
        else
            outlen = nlen;
        b=new byte[outlen];
        outb=new byte[outlen];

        for(n=0;n<nlen-1;n++)
        {
            b[n]=temp_b[n];
        }

        outb=b.clone();

        for( n = 0; n<=outlen - 8 ;n=n+ 8)
        {
            for (i = 0; i < 8; i++) temp[i] = b[i + n];
            EnCode(temp, outtemp, Key);
            for( i = 0 ;i<8;i++) outb[i + n] = outtemp[i];
        }

        outstring = "";
        for (n = 0 ;n<= outlen - 1;n++)
        {
            outstring = outstring +myhex(outb[n]) ;
        }
        return outstring;
    }

    //使用增强算法，解密字符串，用软件的方式
    public String  StrDec(String InString , String Key)
    {

        byte [] b,outb;
        byte [] temp=new byte[8],outtemp=new byte[8];
        int n,i,nlen,outlen;
        String temp_string;


        nlen = InString.length();
        if( nlen < 16 ) outlen = 16;
        outlen = nlen / 2;
        b=new byte[outlen];
        outb=new byte[outlen];

        i = 0;
        for (n = 1 ;n<= nlen ;n=n+2)
        {
            temp_string = InString.substring(n-1, n-1+2);
            b[i] = (byte)HexToInt(temp_string);
            i = i + 1;
        }

        outb=b.clone();

        for( n = 0; n<=outlen - 8 ;n=n+ 8)
        {
            for (i = 0; i < 8; i++) temp[i] = b[i + n];
            DeCode(temp, outtemp, Key);
            for( i = 0 ;i<8;i++) outb[i + n] = outtemp[i];
        }

        try
        {
            return new String(outb,"gb2312");
        }
        catch (UnsupportedEncodingException e) {
            lasterror=-1012;
            return null;
        }
    }

    //使用增强算法，加密字节数组，用软件的方式
    public void EnCode(byte[] inb, byte[] outb,  String Key )
    {

        long cnDelta,y,z,a,b,c,d,temp_2;
        long [] buf=new long[16];
        int n,i,nlen;
        long sum;
        long temp,temp_1;
        long mask=4294967295L;

        //UInt32 temp, temp_1;
        String temp_String ;


        cnDelta = 2654435769L;
        sum = 0;

        nlen = Key.length();
        i = 0;
        for( n = 1 ;n<= nlen ;n=n+2)
        {
            temp_String =Key.substring(n-1, n-1+2);
            buf[i] =HexToInt(temp_String);
            i = i + 1;
        }
        a = 0 ; b = 0 ; c = 0 ; d = 0;
        for(n = 0;n<=3;n++)
        {
            a = (buf[n] << (n * 8)) | a;
            b = (buf[n + 4] << (n * 8)) | b;
            c = (buf[n + 4 + 4] << (n * 8)) | c;
            d = (buf[n + 4 + 4 + 4] << (n * 8)) | d;
        }


        y = 0;
        z = 0;
        for(n = 0;n<=3;n++)
        {
            temp_2 = conver(inb[n]);
            y = (temp_2 << (n * 8)) | y;
            temp_2 = conver(inb[n + 4]);
            z = (temp_2 << (n * 8)) | z;
        }


        n = 32;

        while (n > 0)
        {
            sum = (cnDelta + sum)& mask;

            temp = (z << 4) & mask;
            temp = (temp + a) & mask;
            temp_1 = (z + sum) & mask;
            temp = (temp ^ temp_1) & mask;
            temp_1 = (z >> 5) & mask;
            temp_1 = (temp_1 + b) & mask;
            temp = (temp ^ temp_1) & mask;
            temp = (temp + y) & mask;
            y = temp & mask;
            /*y += ((z << 4) + a) ^ (z + sum) ^ ((z >> 5) + b); */

            temp = (y << 4) & mask;
            temp = (temp + c) & mask;
            temp_1 = (y + sum) & mask;
            temp = (temp ^ temp_1) & mask;
            temp_1 = (y >> 5) & mask;
            temp_1 = (temp_1 + d) & mask;
            temp = (temp ^ temp_1) & mask;
            temp = (z + temp) & mask;
            z = temp & mask;
            /* z += ((y << 4) + c) ^ (y + sum) ^ ((y >> 5) + d); */
            n = n - 1;

        }
        for(n = 0;n<=3;n++)
        {
            outb[n] = (byte)((y >>> (n * 8)) & 255);
            outb[n + 4] =(byte)((z >>> (n * 8)) & 255);
        }
    }
    //使用增强算法，解密字节数组，用软件的方式
    public void DeCode(byte[] inb, byte[] outb,  String Key )
    {

        long cnDelta,y,z,a,b,c,d,temp_2;
        long [] buf=new long[16];
        int n,i,nlen;
        long sum;
        long temp,temp_1;

        long mask=4294967295L;

        //UInt32 temp, temp_1;
        String temp_String ;


        cnDelta = 2654435769L;
        sum = 3337565984L;

        nlen = Key.length();
        i = 0;
        for( n = 1 ;n<= nlen ;n=n+2)
        {
            temp_String =Key.substring(n-1, n-1+2);
            buf[i] =HexToInt(temp_String);
            i = i + 1;
        }
        a = 0 ; b = 0 ; c = 0 ; d = 0;
        for(n = 0;n<=3;n++)
        {
            a = (buf[n] << (n * 8)) | a;
            b = (buf[n + 4] << (n * 8)) | b;
            c = (buf[n + 4 + 4] << (n * 8)) | c;
            d = (buf[n + 4 + 4 + 4] << (n * 8)) | d;
        }


        y = 0;
        z = 0;
        for(n = 0;n<=3;n++)
        {
            temp_2 = conver(inb[n]);
            y = (temp_2 << (n * 8)) | y;
            temp_2 = conver(inb[n + 4]);
            z = (temp_2 << (n * 8)) | z;
        }


        n = 32;

        while (n > 0)
        {


            temp = (y << 4) & mask;
            temp = (temp + c) & mask;
            temp_1 = (y + sum) & mask;
            temp = (temp ^ temp_1) & mask;
            temp_1 = (y >> 5) & mask;
            temp_1 = (temp_1 + d) & mask;
            temp = (temp ^ temp_1) & mask;
            temp = (z - temp) & mask;
            z = temp & mask;
            /* z += ((y << 4) + c) ^ (y + sum) ^ ((y >> 5) + d); */

            temp = (z << 4) & mask;
            temp = (temp + a) & mask;
            temp_1 = (z + sum) & mask;
            temp = (temp ^ temp_1) & mask;
            temp_1 = (z >> 5) & mask;
            temp_1 = (temp_1 + b) & mask;
            temp = (temp ^ temp_1) & mask;
            temp = (y - temp ) & mask;
            y = temp & mask;
            /*y += ((z << 4) + a) ^ (z + sum) ^ ((z >> 5) + b); */

            sum = (sum-cnDelta)& mask;
            n = n - 1;

        }
        for(n = 0;n<=3;n++)
        {
            outb[n] = (byte)((y >>> (n * 8)) & 255);
            outb[n + 4] =(byte)((z >>> (n * 8)) & 255);
        }
    }

    public int CheckKeyByFindort_2()
    {
        //使用普通算法一查找指定的加密锁
        FindPort_2(0, 1, 1729140358);
        return (int)lasterror;
    }


    //使用带长度的方法从指定的地址读取字符串
    private String ReadStringEx(int addr, UsbDevice DevicePath)
    {
        int nlen, ret;
        //先从地址0读到以前写入的字符串的长度
        ret = YReadEx((short)addr, (short)1, "AE8E0504", "CBE0F2CD", DevicePath);
        if (ret != 0) return "";
        nlen = GetBuf(0);
        //再读取相应长度的字符串
        return NewReadString(addr + 1, nlen, "AE8E0504", "CBE0F2CD", DevicePath);

    }
    //使用从储存器读取相应数据的方式检查是否存在指定的加密锁
    public int CheckKeyByReadEprom()
    {
        int n;
        UsbDevice DevicePath = null;//用于储存加密锁所在的路径
        String outString = "";
        return 1;//如果没有使用这个功能，直接返回1
//	        for (n = 0; n < 255; n++)
//	        {
//	            DevicePath= FindPort(n );
//	            if (lasterror != 0) return (int)lasterror;
//				outString = ReadStringEx(0, DevicePath);
//				if ((lasterror == 0) && (outString.compareTo("") == 0)) return 0;
//	        }
//	        return -92;
    }
    //使用增强算法一检查加密锁，这个方法可以有效地防止仿真
    public int CheckKeyByEncstring()
    {
        //推荐加密方案：生成随机数，让锁做加密运算，同时在程序中端使用代码做同样的加密运算，然后进行比较判断。

        int n;
        UsbDevice DevicePath = null;//用于储存加密锁所在的路径
        String InString;

        return 1;//如果没有使用这个功能，直接返回1
//	        int number1=(int)(Math.random()*65535)+1;
//	        int number2=(int)(Math.random()*65535)+1;
//
//	        InString = (new   Integer(number1)).toString() + (new   Integer(number2)).toString();
//
//	        for (n = 0; n < 255; n++)
//	        {
//	           DevicePath= FindPort(n );
//	            if (lasterror != 0) return (int)lasterror;
//	            if (Sub_CheckKeyByEncString(InString, DevicePath) == 0) return 0;
//	        }
//	        return -92;
    }

    private int Sub_CheckKeyByEncString(String InString, UsbDevice DevicePath)
    {
        //使用增强算法一对字符串进行加密
        int ret;
        String outString = "";
        String outString_2;
        outString = EncString(InString,DevicePath);
        if (lasterror != 0) return (int)lasterror;
        outString_2 = StrEnc(InString, "");
        if (outString_2.compareTo(outString) == 0)//比较结果是否相符
        {
            ret = 0;
        }
        else
        {
            ret = -92;
        }
        return ret;
    }

    //使用增强算法二检查是否存在对应的加密锁
    public int CheckKeyByEncstring_New()
    {
        int n;
        UsbDevice DevicePath = null;//用于储存加密锁所在的路径
        String outString = "";
        return 1;//如果没有使用这个功能，直接返回1
        //@NoSupNewKeyEx  return 2;//如果该锁不支持这个功能，直接返回2
//	        for (n = 0; n < 255; n++)
//	        {
//	            DevicePath= FindPort(n );
//	            if (lasterror != 0) return (int)lasterror;
//				outString = EncString_New("123456",DevicePath);
//				if ((lasterror == 0) && (outString.compareTo("@DEncStr") == 0)) return 0;
//	        }
//	        return -92;
    }



}
