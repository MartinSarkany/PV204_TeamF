/*
 *  TestC_CSV in org.jpws.front.util
 *  file: TestC_CSV.java
 * 
 *  Project Jpws-Front
 *  @author Wolfgang Keller
 *  Created 28.11.2005
 *  Version
 * 
 *  Copyright (c) 2011 by Wolfgang Keller, Munich, Germany
 * 
 This program is not freeware software but copyright protected to the author(s)
 stated above. However, you can use, redistribute and/or modify it under the terms 
 of the GNU General Public License as published by the Free Software Foundation, 
 version 2 of the License.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 Place - Suite 330, Boston, MA 02111-1307, USA, or go to
 http://www.gnu.org/copyleft/gpl.html.
 */

package org.jpws.front.util;

import junit.framework.TestCase;

public class TestC_CSV extends TestCase
{

public void test_EncodeField ()
{
   String result, input;
   
   
   input = null;
   result = Util.CSV.quoteText( input );
   assertTrue( "CSV quote NULL", result == null );
   
   input = "";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote EMPTY", result, "\"\"" );
   
   input = " ";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote ONE BLANK", result, "\" \"" );
   
   input = "X";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote ONE X", result, "\"X\"" );
   
   input = "ABCDEFGabcdefg";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote VALUE", result, "\"ABCDEFGabcdefg\"" );
   
   input = "A B C D E F G a b c d e f g";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote VALUE", result, "\"A B C D E F G a b c d e f g\"" );
   
   input = "AB  CD E F    ";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote VALUE", result, "\"AB  CD E F    \"" );
   
   input = "     AB  CD E F    ";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote VALUE", result, "\"     AB  CD E F    \"" );
   
   input = "Araunibala,Knöterich";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote VALUE-Komma", result, "\"Araunibala,Knöterich\"" );
   
   input = "Araunibala , Knöterich";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote VALUE, Komma", result, "\"Araunibala , Knöterich\"" );
   
   input = "Araunibala\r\nKnöterich";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote VALUE-CRLF 1", result, "\"Araunibala\r\nKnöterich\"" );
   
   input = "Araunibala \r\n, Knöterich";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote VALUE-CRLF 2", result, "\"Araunibala \r\n, Knöterich\"" );
   
   input = "\"";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote SINGLE QUOTE", result, "\"\"\"\"" );
   
   input = "\"\"";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote DOUBLE QUOTE", result, "\"\"\"\"\"\"" );
   
   input = "\"Textab fassung\"";
   result = Util.CSV.quoteText( input );
//System.out.println( "[" + input + "] / [" + result + "]");    
   assertEquals( "CSV quote QUOTE TEXT", result, "\"\"\"Textab fassung\"\"\"" );
   
   input = "Abraham \"Textab fassung\" neutral";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote QUOTE TEXT MIDDLE", result, "\"Abraham \"\"Textab fassung\"\" neutral\"" );
   
   input = "Abraham \"Textab\r\nfassung\" neutral";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote QUOTE TEXT MIDDLE CR", result, "\"Abraham \"\"Textab\r\nfassung\"\" neutral\"" );
   
   // Iterated Cases
   input = "\"Abraham \"\"Textab\r\nfassung\"\" neutral\"";
   result = Util.CSV.quoteText( input );
   assertEquals( "CSV quote QUOTE TEXT ITER 1", result, "\"\"\"Abraham \"\"\"\"Textab\r\nfassung\"\"\"\" neutral\"\"\"" );
   
}

public void test_DecodeField ()
{
   String result, input;
   
   
   input = null;
   result = Util.CSV.unquoteText( input );
   assertTrue( "CSV unquote NULL", result == null );
   
   input = "";
   try { 
      result = Util.CSV.unquoteText( input );
      fail( "CSV unquote EMPTY, missing exception" );
   }
   catch ( Exception e )
   {
      assertTrue( "CSV unquote EMPTY, exception type", e instanceof IllegalArgumentException  );
   }
   
   input = " ";
   try { 
      result = Util.CSV.unquoteText( input );
      fail( "CSV unquote ONE CHAR, missing exception" );
   }
   catch ( Exception e )
   {
      assertTrue( "CSV unquote ONE CHAR, exception type", e instanceof IllegalArgumentException  );
   }
   
   input = " XCSVX ";
   try { 
      result = Util.CSV.unquoteText( input );
      fail( "CSV unquote MISS QUOTES, missing exception" );
   }
   catch ( Exception e )
   {
      assertTrue( "CSV unquote MISS QUOTES, exception type", e instanceof IllegalArgumentException  );
   }
   
   input = "\"XCSVX\"";
   result = Util.CSV.unquoteText( input );
   assertEquals( "CSV unquote VAL 1", result, "XCSVX" );
   
   input = "\"\"\"\"";
   result = Util.CSV.unquoteText( input );
   assertEquals( "CSV quote SINGLE QUOTE", result, "\"" );

   input = "\" \"\" \"";
   result = Util.CSV.unquoteText( input );
   assertEquals( "CSV quote enclosed SINGLE QUOTE", result, " \" " );

   input = "  \" \"\" \"  ";
   result = Util.CSV.unquoteText( input );
   assertEquals( "CSV quote untrimmed enclosed SINGLE QUOTE", result, " \" " );

   input = "\"\\\"\"";
   result = Util.CSV.unquoteText( input );
   assertEquals( "CSV quote BACKSLASH QUOTE", result, "\\\"" );

   input = " \"Araunibala,Knöterich\" ";
   result = Util.CSV.unquoteText( input );
   assertEquals( "CSV unquote VALUE-Komma 1", result, "Araunibala,Knöterich" );
   
   input = " \"Araunibala\",Knöterich\" ";
   result = Util.CSV.unquoteText( input );
   assertEquals( "CSV unquote VALUE-Komma 2", result, "Araunibala\",Knöterich" );
   
   try {
      input = " \"Araunibala,Knöterich ";
      result = Util.CSV.unquoteText( input );
      fail( "CSV unquote, exception on illegal arg 1, missing" );
   }
   catch ( Exception e )
   {
      assertTrue( "CSV unquote, exception on illegal arg 1, type", 
            e instanceof IllegalArgumentException );
   }
   
   try {
      input = " Araunibala,Knöterich\" ";
      result = Util.CSV.unquoteText( input );
      fail( "CSV unquote, exception on illegal arg 2, missing" );
   }
   catch ( Exception e )
   {
      assertTrue( "CSV unquote, exception on illegal arg 2, type", 
            e instanceof IllegalArgumentException );
   }
   
   input = "\"Araunibala , Knöterich\"";
   result = Util.CSV.unquoteText( input );
   assertEquals( "CSV unquote VALUE, Komma 2", result, "Araunibala , Knöterich" );

   input = "\"Araunibal\\a,Kn\\öt\\erich\"";
   result = Util.CSV.unquoteText( input );
   assertEquals( "CSV unquote VALUE-ESCAPE 1", result, "Araunibal\\a,Kn\\öt\\erich" );
   
   input = "\"Abraham \"\"Textab\r\nfassung\"\" neutral\"";
   result = Util.CSV.unquoteText( input );
   assertEquals( "CSV unquote QUOTE TEXT MIDDLE CR", result, "Abraham \"Textab\r\nfassung\" neutral" );
   
   // Iterated Cases
   input = "\"\"\"Abraham \"\"\"\"Textab\r\nfassung\"\"\"\" neutral\"\"\"";
   result = Util.CSV.unquoteText( input );
   assertEquals( "CSV unquote QUOTE TEXT ITER 1", result, "\"Abraham \"\"Textab\r\nfassung\"\" neutral\"" );
}

public void test_SearchQuoteEnd ()
{
   String result, input;
   int i;
   
   input = null;
   try { 
      i = Util.CSV.searchQuoteEnd( input, 1 ); 
      fail( "CSV searchQuoteEnd NULL, missing exception" );
      }
   catch ( Exception e )
   {
      assertTrue( "CSV searchQuoteEnd NULL, exception type", e instanceof NullPointerException );
   }
   
   input = "";
   try { 
      i = Util.CSV.searchQuoteEnd( input, 0 ); 
      fail( "CSV searchQuoteEnd EMPTY, missing exception" );
      }
   catch ( Exception e )
   {
      assertTrue( "CSV searchQuoteEnd EMPTY, exception type", e instanceof IllegalArgumentException );
   }
   
   input = "XCVSJ";
   try { 
      i = Util.CSV.searchQuoteEnd( input, 0 ); 
      fail( "CSV searchQuoteEnd UNQUOTED S0, missing exception" );
      }
   catch ( Exception e )
   {
      assertTrue( "CSV searchQuoteEnd UNQUOTED S0, exception type", e instanceof IllegalArgumentException );
   }
   
   input = "XCVSJ";
   i = Util.CSV.searchQuoteEnd( input, 1 ); 
   assertEquals( "CSV searchQuoteEnd ONE QUOTE", -1, i );
   
   input = "\"";
   try { 
      i = Util.CSV.searchQuoteEnd( input, 0 ); 
      fail( "CSV searchQuoteEnd ONE CHAR 0, missing exception" );
      }
   catch ( Exception e )
   {
      assertTrue( "CSV searchQuoteEnd ONE CHAR 0, exception type", e instanceof IllegalArgumentException );
   }

   input = "\"";
   try { 
      i = Util.CSV.searchQuoteEnd( input, 1 ); 
      fail( "CSV searchQuoteEnd ONE CHAR 1, missing exception" );
      }
   catch ( Exception e )
   {
      assertTrue( "CSV searchQuoteEnd ONE CHAR 1, exception type", e instanceof IllegalArgumentException );
   }

   input = "X";
   try { 
      i = Util.CSV.searchQuoteEnd( input, 1 ); 
      fail( "CSV searchQuoteEnd FAIL START, missing exception" );
      }
   catch ( Exception e )
   {
      assertTrue( "CSV searchQuoteEnd FAIL START, exception type", e instanceof IllegalArgumentException );
   }

   input = " \"";
   i = Util.CSV.searchQuoteEnd( input, 1 ); 
   assertEquals( "CSV searchQuoteEnd ONE QUOTE", i, 2 );

   input = "\"\"\"Abraham \"\"\"\"Textab\r\nfassung\"\"\"\" neutral\"\"\"";
   i = Util.CSV.searchQuoteEnd( input, 1 ); 
   assertEquals( "CSV searchQuoteEnd TEST VALUE A", 45, i );

   input = "\"\"Abraham \"\"\"\"Textab\r\nfassung\"\"\"\" neutral\"\"\"";
   i = Util.CSV.searchQuoteEnd( input, 1 ); 
   assertEquals( "CSV searchQuoteEnd TEST VALUE B", 2, i );

   input = "\"\"\"Abraham \"\"Textab\r\nfa\\ssung\"  \"\"\"\" neutral\"\"\"";
   i = Util.CSV.searchQuoteEnd( input, 1 ); 
   assertEquals( "CSV searchQuoteEnd TEST VALUE C", 30, i );

}

public void test_WriteLine ()
{
   String result, expect;
   String[] input;
   int i;
   
   try { 
      Util.CSV.encodeLine( null, ',' ); 
      fail( "CSV encodeLine NULL INPUT, missing exception" );
      }
   catch ( Exception e )
   {
      assertTrue( "CSV encodeLine NULL INPUT, exception type", e instanceof NullPointerException );
   }

   input = new String[0];
   expect = "";
   result = Util.CSV.encodeLine( input, ',' );
   assertEquals( "CSV encodeLine EMPTY INPUT", expect, result );
   
   input = new String[] { "", "", null, "", "" };
   expect = ",,,,";
   result = Util.CSV.encodeLine( input, ',' );
   assertEquals( "CSV encodeLine EMPTY STRINGS INPUT", expect, result );
   
   input = new String[] { "Abrak", " kad abra ", null, "", "   " };
   expect = "Abrak,\" kad abra \",,,\"   \"";
   result = Util.CSV.encodeLine( input, ',' );
   assertEquals( "CSV encodeLine VALUE 1", expect, result );
   
   input = new String[] { "Abr\" drei\" ak", "kannte mich nicht", "\"Kälber mast\"", " \"Hexen hunde \" " };
   expect = "\"Abr\"\" drei\"\" ak\",kannte mich nicht,\"\"\"Kälber mast\"\"\",\" \"\"Hexen hunde \"\" \"";
   result = Util.CSV.encodeLine( input, ',' );
   assertEquals( "CSV encodeLine VALUE 2", expect, result );
   
   input = new String[] { "\r\n", "kad\r\nabra", "kad\nabra", " \"kad\nabra" };
   expect = "\"\r\n\",\"kad\r\nabra\",\"kad\nabra\",\" \"\"kad\nabra\"";
   result = Util.CSV.encodeLine( input, ',' );
   assertEquals( "CSV encodeLine VALUE 3", expect, result );
   
   input = new String[] { ",", ",,", ", ,", "kad,nabra", "kad, ,abra", "mulux,", "   ,tulix", "\"kad, nabra\"" };
   expect = "\",\",\",,\",\", ,\",\"kad,nabra\",\"kad, ,abra\",\"mulux,\",\"   ,tulix\",\"\"\"kad, nabra\"\"\"";
//   System.out.println( "Expected: " + expect );
   result = Util.CSV.encodeLine( input, ',' );
//   System.out.println( "Resulted: " + result );
   assertEquals( "CSV encodeLine VALUE 4", expect, result );
   
}

public void test_ReadLine ()
{
   String[] result, expect;
   String input;
   int i;
 
   try { 
      Util.CSV.decodeLine( null, 0, ',' ); 
      fail( "CSV decodeLine NULL INPUT, missing exception" );
      }
   catch ( Exception e )
   {
      assertTrue( "CSV decodeLine NULL INPUT, exception type", e instanceof NullPointerException );
   }

   input = "";
   expect = new String[0];
   result = Util.CSV.decodeLine( input, 0, ',' );
   assertTrue( "CSV decodeLine EMPTY INPUT", equalArrays( expect, result ) );
   
   
   input = "Abrak,\" kad abra \",,,\"   \"";
   expect = new String[] { "Abrak", " kad abra ", "", "", "   " };
   result = Util.CSV.decodeLine( input, 0, ',' );
   assertTrue( "CSV decodeLine VALUE 1", equalArrays( expect, result ) );

   input = "\"Abr\"\" drei\"\" ak\",kannte mich nicht,\"\"\"Kälber mast\"\"\",\" \"\"Hexen hunde \"\" \"";
   expect = new String[] { "Abr\" drei\" ak", "kannte mich nicht", "\"Kälber mast\"", " \"Hexen hunde \" " };
   result = Util.CSV.decodeLine( input, 0, ',' );
   assertTrue( "CSV decodeLine VALUE 2", equalArrays( expect, result ) );

   input = "\"\r\n\",\"kad\r\nabra\",\"kad\nabra\",\" \"\"kadabra\n\"";
   expect = new String[] { "\r\n", "kad\r\nabra", "kad\nabra", " \"kadabra\n" };
   result = Util.CSV.decodeLine( input, 0, ',' );
   assertTrue( "CSV decodeLine VALUE 4", equalArrays( expect, result ) );

   input = "\",\",\",,\",\", ,\",\"kad,nabra\",\"kad, ,abra\",\"mulux,\",\"   ,tulix\",\"\"\"kad, nabra\"\"\"";
   expect = new String[] { ",", ",,", ", ,", "kad,nabra", "kad, ,abra", "mulux,", "   ,tulix", "\"kad, nabra\"" };
   result = Util.CSV.decodeLine( input, 0, ',' );
   assertTrue( "CSV decodeLine VALUE 4", equalArrays( expect, result ) );

   input = "Hans im Glück,,";
   expect = new String[] { "Hans im Glück", "", "" };
   result = Util.CSV.decodeLine( input, 0, ',' );
   assertTrue( "CSV decodeLine VALUE 5", equalArrays( expect, result ) );

   input = ",,Hans im Glück";
   expect = new String[] { "", "", "Hans im Glück" };
   result = Util.CSV.decodeLine( input, 0, ',' );
   assertTrue( "CSV decodeLine VALUE 6", equalArrays( expect, result ) );

   input = ",,";
   expect = new String[] { "", "", "" };
   result = Util.CSV.decodeLine( input, 0, ',' );
   assertTrue( "CSV decodeLine VALUE 7", equalArrays( expect, result ) );

   input = ",";
   expect = new String[] { "", "" };
   result = Util.CSV.decodeLine( input, 0, ',' );
   assertTrue( "CSV decodeLine VALUE 8", equalArrays( expect, result ) );

   input = "\"\",\"\",\"\"";
   expect = new String[] { "", "", "" };
   result = Util.CSV.decodeLine( input, 0, ',' );
   assertTrue( "CSV decodeLine VALUE 9", equalArrays( expect, result ) );

   
   
}

private boolean equalArrays ( String[] a, String[] b )
{
   if ( a == b )
      return true;
   
   if ( ( a == null & b != null ) | ( a != null & b == null ) )
      return false;
   
   if ( a.length != b.length )
   {
      System.out.println( "EXPECTED: "  );
      printStrings( a );
      System.out.println( "ENCOUNTERED: " );
      printStrings( b );
      return false;
   }
   
   for ( int i = 0; i < a.length; i++ )
      if ( !a[i].equals( b[i]) )
      {
         System.out.println( "EXPECTED: "  );
         printStrings( a );
         System.out.println( "ENCOUNTERED: " );
         printStrings( b );
         return false;
      }
   return true;
}

private void printStrings ( String[] sa )
{
   for ( int i = 0; i < sa.length; i++ )
      System.out.println( " -: [" + sa[i] + "]");
}

}
