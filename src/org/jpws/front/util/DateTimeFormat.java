package org.jpws.front.util;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.jpws.front.Global;


public class DateTimeFormat 
{
    public enum DateFormat { standard_short, standard_long, standard_text, technical, 
        USA_short, USA_long, USA_text, compressed, VM_default; 
        
        public static DateFormat forName ( String name ) {
            for ( DateFormat f : DateFormat.values() ) {
                if ( f.name().equals(name) ) 
                   return f; 
            }
            return null;
        }
    }
        
    public enum TimeFormat { standard_short, standard_long, English_short, English_long, 
        compressed_short, compressed_long, empty, VM_default; 
        
        public static TimeFormat forName ( String name ) {
            for ( TimeFormat f : TimeFormat.values() ) {
                if ( f.name().equals(name) ) 
                   return f; 
            }
            return null;
        }
    }
    
    private DateTimeFormat.DateFormat dateType;
    private DateTimeFormat.TimeFormat timeType;

    /** Creates a date-time format with a date and a clock rendering.
     * 
     * @param dType <code>DateFormat</code> date format choice
     * @param tType <code>TimeFormat</code> clock format choice
     */
    public DateTimeFormat ( DateTimeFormat.DateFormat dType, DateTimeFormat.TimeFormat tType ) {
        if ( dType == null | tType == null ) {
            throw new NullPointerException();
        }
        dateType = dType;
        timeType = tType;
    }

    /** Creates a date-time format with a date and an empty clock rendering.
     * 
     * @param dType <code>DateFormat</code> date format choice
     */
    public DateTimeFormat ( DateTimeFormat.DateFormat dType ) {
        this( dType, TimeFormat.empty );
    }

    /** Creates a date-time format with a VM-default date and the given
     * clock rendering.
     * 
     * @param tType <code>TimeFormat</code> time format choice
     */
    public DateTimeFormat ( DateTimeFormat.TimeFormat tType ) {
        this( DateFormat.VM_default, tType );
    }

    /** Creates a date-time format with "VM default" setting
     * for both date and time format. 
     */
    public DateTimeFormat() {
        this( DateFormat.VM_default, TimeFormat.VM_default );
    }

    public DateTimeFormat.DateFormat getDateType () {
        return dateType;
    }
    
    public DateTimeFormat.TimeFormat getTimeType () {
        return timeType;
    }
    
    /** Renders a date output, appropriate
     *  to the format selections of this date-time format.
     *  
     * @param time long time millis (UT)
     * @param zone TimeZone time zone that applies to rendering
     *        <b>null</b> invokes VM default
     * @return String date text
     */  
    public String renderDate ( long time, TimeZone zone ) {
        if ( zone == null ) {
            zone = TimeZone.getDefault();
        }

        int day, month, year;
        String sday, sday_txt, smonth, smonth_txt, syear_long, syear_short, hstr;
        String[] monthTexts;
        
        GregorianCalendar cal = new GregorianCalendar( zone );
        cal.setTimeInMillis( time );
        hstr = ResourceLoader.getDisplay("months_short");
        monthTexts = Util.CSV.decodeLine( hstr, 0, ',' );        
                
        year = cal.get( GregorianCalendar.YEAR );
        syear_long = Util.number( year, 4 );
        syear_short = syear_long.substring(2);

        month = cal.get( GregorianCalendar.MONTH );
        smonth = Util.number( month+1, 2 );
        smonth_txt = monthTexts[ month ];

        day = cal.get( GregorianCalendar.DAY_OF_MONTH );
        sday = Util.number(day, 2);
        sday_txt = sday.charAt(0) == '0' ? (" ".concat(sday.substring(1))) : sday;
        
        switch (dateType) {

        case standard_long:
            hstr = sday + "." + smonth + "." + syear_long; 
            break;
        case standard_short:
            hstr = sday + "." + smonth + "." + syear_short; 
            break;
        case standard_text:
            hstr = sday_txt + " " + smonth_txt + " " + syear_long; 
            break;
        case technical:
            hstr = syear_long + "-" + smonth + "-" + sday; 
            break;
        case compressed:
            hstr = syear_long + smonth + sday; 
            break;
        case USA_long:
            hstr = smonth + "-" + sday + "-" + syear_long; 
            break;
        case USA_short:
            hstr = smonth + "-" + sday + "-" + syear_short; 
            break;
        case USA_text:
            hstr = smonth_txt + " " + sday_txt + ", " + syear_long; 
            break;
        case VM_default:
            java.text.DateFormat fo = java.text.DateFormat.getDateInstance();
            fo.setTimeZone( zone );
            hstr = fo.format( new Date( time ) );
            break;

        default : hstr = "?";
        }
        
        return hstr;
    }
    
    /** Renders a clock output, appropriate
     *  to the format selections of this date-time format.
     *  
     * @param time long time millis (UT)
     * @param zone TimeZone time zone that applies to rendering;
     *        <b>null</b> invokes VM default
     * @return String clock text
     */  
    public String renderTime ( long time, TimeZone zone ) {
        if ( zone == null ) {
            zone = TimeZone.getDefault();
        }

        int hour, hour_halfday, minute, second;
        String shour, shour_halfday, sminute, ssecond, sdayhalf, hstr;
        boolean isPM;
        
        GregorianCalendar cal = new GregorianCalendar( zone );
        cal.setTimeInMillis( time );
        
        hour = cal.get( GregorianCalendar.HOUR_OF_DAY );
        isPM = hour >= 12;
        sdayhalf = isPM ? "PM" : "AM";
        hour_halfday = hour >= 13 ? hour-12 : hour;   
        minute = cal.get( GregorianCalendar.MINUTE );
        second = cal.get( GregorianCalendar.SECOND );

        shour = Util.number( hour, 2);
        shour_halfday = Util.number( hour_halfday, 2 );
        sminute = Util.number(minute, 2);
        ssecond = Util.number(second, 2);
        
        switch (timeType) {

        case standard_long:
            hstr = shour + ":" + sminute + ":" + ssecond; 
            break;
        case standard_short:
            hstr = shour + ":" + sminute; 
            break;
        case English_long:
            hstr = shour_halfday + ":" + sminute + ":" + ssecond + " " + sdayhalf; 
            break;
        case English_short:
            hstr = shour_halfday + ":" + sminute + " " + sdayhalf; 
            break;
        case compressed_long:
            hstr = shour + sminute + ssecond; 
            break;
        case compressed_short:
            hstr = shour + sminute; 
            break;
        case empty:
            hstr = ""; 
            break;
        case VM_default:
            java.text.DateFormat fo = java.text.DateFormat.getTimeInstance();
            fo.setTimeZone( zone );
            hstr = fo.format( new Date( time ) );
            break;

        default : hstr = "?";
        }

        return hstr;
    }
    
    /** Renders a concatenation of date and clock output, appropriate
     * to the format selections of this date-time format.
     * 
     * @param time long time millis (UT)
     * @param zone TimeZone time zone that applies to rendering
     *        <b>null</b> invokes VM default
     * @return String date-time text
     */
    public String renderDateTime ( long time, TimeZone zone ) {
        String dt = renderDate( time, zone );
        String tm = renderTime( time, zone );
        
        if ( (timeType == TimeFormat.compressed_long |
              timeType == TimeFormat.compressed_short |    
              timeType == TimeFormat.empty)    
             & dateType == DateFormat.compressed ) {
            return dt.concat(tm);
        }
        return dt + " " + tm;
    }

    private static String testLine_renderTime ( long time, DateTimeFormat.DateFormat df, DateTimeFormat.TimeFormat tf ) {
        String result, hstr;
        DateTimeFormat fo = new DateTimeFormat( df, tf );
        hstr = fo.renderDateTime(time, null);
        result = "DF=" + df + ", TF=" + tf + " :  " + hstr;
        return result;
    }
    
    public static void test_output () {
        @SuppressWarnings("deprecation")
        long tm = new Date( 112, 9, 5 ).getTime() + 14*Global.HOUR + 35*Global.MINUTE + 12000;
        
        System.out.println();
        System.out.println( "*** TEST OUTPUT OF DATE-TIME FORMATS ***" );
        
        for ( DateTimeFormat.DateFormat df : DateFormat.values() ) {
            for ( DateTimeFormat.TimeFormat tf : TimeFormat.values() ) {
                System.out.println( "   " + testLine_renderTime( tm, df, tf ));
            }
        }
        System.out.println();
    }
    
}