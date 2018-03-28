package com.amsu.amsuinsolebletest.util;

/**
 * Created by HP on 2016/12/12.
 */



public class EcgFilterUtil_1 {
    private  final int SAMPRATE = 150;
    private  final float PI = 3.1415926f;


    ////陷波
    private  final int FILT_TIME= 2;
    private  final int  M_NOTCH = 60;

    private float Buffer_X_org[] = new float[M_NOTCH-1];
    private double Buffer_X[][] = new double[FILT_TIME][M_NOTCH+1];
    private double Buffer_Y[][] = new double[FILT_TIME][2];
    private int Pos_X[] = new int[FILT_TIME];
    private int Pos_X_org;
    private double Coef_Y;
    private double Gain;
    private char Filt_Mark_notch = 0xF;
    /*
        typedef struct FILTINFO
        {
            int  nFilterData;  //滤波后值
            int  nDelay;   //延迟点数
        }sFiltInfo;
     */
    public class FILTINFO{
        int  nFilterData;  //滤波后值
        int  nDelay;   //延迟点数
    }


    int Initfilter()
    {
        Filt_Mark_notch = 0xF;
        return 0;
    }


    public int Filter_Init_notch(int mark)
    {

/////陷波
        double w;
        w = Math.atan(1)*4;

        Filt_Mark_notch= (char)mark;

        if(1==Filt_Mark_notch)
        {
            w = 2*w *50/SAMPRATE;
        }

/*
        memset(Buffer_X,0,sizeof(Buffer_X));
        memset(Buffer_Y,0,sizeof(Buffer_Y));
        memset(Pos_X,0,sizeof(Pos_X));
*/
        for(int j=0; j<FILT_TIME;j++) {
            for(int k=0; k<M_NOTCH+1;k++) {
                Buffer_X[j][k] = 0;
            }
        }

        for(int j=0; j<FILT_TIME;j++) {
            for(int k=0; k<1;k++) {
                Buffer_Y[j][k] = 0;
            }
        }

        for(int j=0; j<FILT_TIME;j++) {
            Pos_X[j] = 0;
        }


        Pos_X_org = 0;

        Coef_Y = 2*Math.cos(w);
        Gain = Math.pow((2*Math.sin(w))/M_NOTCH,FILT_TIME);
        return 1;
    }


    public double Notch_filter(float data,  int filtno)
    {
        double y;

        int posb1;

        Pos_X[filtno]++;
        if(Pos_X[filtno]>M_NOTCH)
        {
            Pos_X[filtno] = 0;
        }

        //报异常改动 java.lang.ArrayIndexOutOfBoundsException: length=61; index=61

        if (Pos_X[filtno]>=M_NOTCH+1){
            return 0;
        }
        Buffer_X[filtno][Pos_X[filtno]] = data;

        if(Pos_X[filtno]-M_NOTCH>=0)
        {
            posb1 = Pos_X[filtno] - M_NOTCH;
        }
        else
        {
            posb1 = Pos_X[filtno] + 1;
        }

        y = data - Buffer_X[filtno][posb1] + Coef_Y*Buffer_Y[filtno][1]
                - Buffer_Y[filtno][0];

        Buffer_Y[filtno][0] = 	Buffer_Y[filtno][1];
        Buffer_Y[filtno][1] = y;

        return y;

    }
    public   int NotchPowerLine(int data,int Filter_Mark)
    {
        int i,posx;
        double y;
        FILTINFO gFilt_result = new FILTINFO();

//        memset(&gFilt_result,0,sizeof(sFiltInfo));

//	if (count == 1)
//		{
//			Initfilter();
//			}
        if(Filt_Mark_notch != Filter_Mark)
        {
            Filter_Init_notch(Filter_Mark);
            return 0;
        }
        if(1!= Filter_Mark)
        {
//		gFilt_result.nFilterData = data;
            return data;
        }

        Pos_X_org++;
        if(Pos_X_org>M_NOTCH-2)
        {
            Pos_X_org = 0;
        }
        Buffer_X_org[Pos_X_org] = (float)(data);

        if(Pos_X_org-(M_NOTCH-2)>=0)
        {
            posx = Pos_X_org-(M_NOTCH-2);
        }
        else
        {
            posx = Pos_X_org + 1;
        }

        y = (double)data;

        for(i=0;i<FILT_TIME;i++)
        {
            y =  Notch_filter((float)y, i);
        }
        y *= Gain;


        gFilt_result.nFilterData = 	 (int)(Buffer_X_org[posx] + y);;
        gFilt_result.nDelay = 	 M_NOTCH-2;

        return gFilt_result.nFilterData;

    }

    //高通滤波器
    private double ecghpa[] = {-0.0568174147470748, -0.0710419364580054,
            -0.0930035878919868, -0.131968518583968, -0.221920016339399,
            -0.668730563936487, 0.668730563936487, 0.221920016339399, 0.131968518583968,
            0.0930035878919868, 0.0710419364580054 ,0.0568174147470748
    };


    private final int ECG_HPASS_LEN=12;

    private double hx0[] = new double[ECG_HPASS_LEN];
    public   int miniEcgFilterHp(int d)
    {
        //输入 int d  待滤波的点
        //输出 int    滤波后的点
        double hy0;

        int i;
        for (i = 0; i < ECG_HPASS_LEN-1; i++)
            hx0[ECG_HPASS_LEN-1 - i] = hx0[ECG_HPASS_LEN-2 - i];
        hx0[0] = d;
        hy0 = 0;
        for (i = 0; i < ECG_HPASS_LEN; i++)
            hy0 += ecghpa[i] * hx0[i];

        return (int) hy0;
    }

    // 低通滤波器
    private double ecglpa[] = {-0.0151121400697321,0.0262591860800819,
            0.0203140144169644, -0.0329292295451105 ,-0.028617248332993,
            0.0441317711289158, 0.0441151980073518, -0.0676575476453283,
            -0.0840037350344892, 0.152862737773564, 0.440636993220775,
            0.440636993220775 ,0.152862737773564, -0.0840037350344892,
            -0.0676575476453283 ,0.0441151980073518, 0.0441317711289158,
            -0.028617248332993, -0.0329292295451105, 0.0203140144169644,
            0.0262591860800819, -0.0151121400697321
    };



    private final int ECG_LPASS_LEN=22;

    private double lx0[] = new double[ECG_LPASS_LEN];
    public  int miniEcgFilterLp(int d)
    {
        //输入 int d  待滤波的点
        //输出 int    滤波后的点
        double ly0;
        int i;
        for (i = 0; i < ECG_LPASS_LEN-1; i++)
            lx0[ECG_LPASS_LEN-1 - i] = lx0[ECG_LPASS_LEN-2 - i];
        lx0[0] = d;
        ly0 = 0;


        for (i = 0; i < ECG_LPASS_LEN; i++)
            ly0 += ecglpa[i] * lx0[i];
        return (int) ly0;
    }


}
