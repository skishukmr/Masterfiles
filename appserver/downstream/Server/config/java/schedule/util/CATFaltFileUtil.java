/**
 * Created by kannan
 * Useage :
 *
 * Change History
 *	Change By	Change Date		Description
 * =============================================================================================
 *  M. W. Jordan        2012/03/10              Added null check to getFormattedTxt, setting the input string to "" if it is null.
 *
 */

package config.java.schedule.util;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
//Change made by Soumya begins
import ariba.util.core.IOUtil;
//Change made by Soumya ends

import ariba.util.core.Date;
import ariba.util.log.Log;


public class CATFaltFileUtil {

    public static String getFileExtDateTime(Date date){
        Date date1 = date;
        Log.customer.debug("received date => "+ date1);
        SimpleDateFormat formatter   = new SimpleDateFormat ("yyyyMMddhhmmss");
        String dateTimeString = formatter.format(date1);
        Log.customer.debug("return dateTimeString for file => "+ dateTimeString);
        return dateTimeString;
    }

    public static String getFormattedTxt(String inputText, int txtLength) {
        if (inputText == null)  inputText = new String("");

        int temp = txtLength - inputText.length();
        String formattedText = "";

        Log.customer.debug("int temp  " + temp);
        if (temp == 0) {
            return inputText;
        }

        if (temp > 0) {
            for (int i = 0; i < temp; i++) {
                inputText = inputText + " ";
            }
        }
        else {
            inputText = inputText.substring(0, txtLength);
        }
        formattedText = inputText;
        Log.customer.debug("formattedText " + formattedText);
        return formattedText;
    }

	public static String getFormattedDate(Date date){
				   Date date1 = date;
				   Log.customer.debug("received date => "+ date1);
				   SimpleDateFormat formatter   = new SimpleDateFormat ("yyyy-MM-dd");
				   String dateTimeString = formatter.format(date1);
				   Log.customer.debug("return dateString for file => "+ dateTimeString);
				   return dateTimeString;
	   }
	public static String getEzFormattedDate(Date date){
		   Date date1 = date;
		   Log.customer.debug("received date => "+ date1);
		   SimpleDateFormat formatter   = new SimpleDateFormat ("yyyyMMdd");
		   String dateTimeString = formatter.format(date1);
		   Log.customer.debug("return dateString for file => "+ dateTimeString);
		   return dateTimeString;
   }
	public static String getEzCtrlFormattedDate(Date date){
		   Date date1 = date;
		   Log.customer.debug("received date => "+ date1);
		   SimpleDateFormat formatter   = new SimpleDateFormat ("yyyy-MM-DD-HH.MM.SS.SSSSSS");
		   String dateTimeString = formatter.format(date1);
		   Log.customer.debug("return dateString for file => "+ dateTimeString);
		   return dateTimeString;
}
	public static String getFormattedDate(Date date, String format){
		   Date date1 = date;
		   Log.customer.debug("received date => "+ date1);
		   SimpleDateFormat formatter   = new SimpleDateFormat (format);
		   String dateTimeString = formatter.format(date1);
		   Log.customer.debug("return dateString for file => "+ dateTimeString);
		   return dateTimeString;
}

	public static String getFormattedNumber(double number) {
			String amtPattern = "0000000000000.00";
			DecimalFormat amtFormatter = new DecimalFormat(amtPattern);
			String formatedAmt = amtFormatter.format(number);
			if (number >= 0.0)
				formatedAmt = "+" + formatedAmt;
			//else
			//	formatedAmt = "-" + formatedAmt;

			return formatedAmt;

		}
	public static String getFormattedNumber(double number, String amtPattern1 ) {
		String amtPattern = amtPattern1;
		DecimalFormat amtFormatter = new DecimalFormat(amtPattern);
		String formatedAmt = amtFormatter.format(number);
		if (number >= 0.0)
			formatedAmt = "+" + formatedAmt;
		//else
			//formatedAmt = "-" + formatedAmt;

		return formatedAmt;

	}
	// Added below funtion by VJS
		public static String getFormattedNumber2(double number, String amtPattern1 ) {
		String amtPattern = amtPattern1;
		DecimalFormat amtFormatter = new DecimalFormat(amtPattern);
		String formatedAmt = amtFormatter.format(number);
		if (number >= 0.0)
			formatedAmt = formatedAmt + " ";
		//else
			//formatedAmt = "-" + formatedAmt;

		return formatedAmt;

	}
	public static String getEzFormattedNumber(double number, String amtPattern1 ) {
		String amtPattern = amtPattern1;
		DecimalFormat amtFormatter = new DecimalFormat(amtPattern);
		String formatedAmt = amtFormatter.format(number);
		return formatedAmt;

	}
	public static String getEzThousandFormattedNumber(double number, String amtPattern1 ) {
		String amtPattern = amtPattern1;
		double numberx = number * 1000;
		DecimalFormat amtFormatter = new DecimalFormat(amtPattern);
		String formatedAmt = amtFormatter.format(numberx);
		return formatedAmt;

	}

	// Below function added by VJS
	public static String addLeadingZeros(String inputNum, int txtlen) {
		String formattedNum = "";
		int temp = txtlen - inputNum.length();
		Log.customer.debug("int temp  " + temp);
		for (int i = 0; i < temp; i++) {
			inputNum = "0" + inputNum;
		}
		formattedNum = inputNum;
		Log.customer.debug("formattedNum " + formattedNum);
		return formattedNum;
	}

	// Below function added by VJS
	public static String addSignLeadingZeros(String inputNum, int txtlen) {
		String formattedNum = "";
		int temp = txtlen - inputNum.length();
		Log.customer.debug("int temp  " + temp);
		for (int i = 0; i < temp; i++) {
			inputNum = "0" + inputNum;
		}
		formattedNum = "+" +inputNum;
		Log.customer.debug("formattedNum " + formattedNum);
		return formattedNum;
	}
	
	// Below function added by VJS
	public static String addEndingZeros(String inputNum, int txtlen) {
		String formattedNum = "";
		int temp = txtlen - inputNum.length();
		Log.customer.debug("int temp  " + temp);
		for (int i = 0; i < temp; i++) {
			inputNum = inputNum + "0";
		}
		formattedNum = inputNum;
		Log.customer.debug("formattedNum " + formattedNum);
		return formattedNum;
	}

	/*
	public static String getAmountInBaseCurrency(double number) {
		String amtPattern = "0000000000.00000";
		DecimalFormat amtFormatter = new DecimalFormat(amtPattern);
		String formatedAmt = amtFormatter.format(number);
		if (number >= 0.0)
			formatedAmt = "+" + formatedAmt;
		else
			formatedAmt = "-" + formatedAmt;

		return formatedAmt;

	} */

   	  /*
      public  static void copyFile(String sourcefile,String destfile)  {
	  		try {
	  			Log.customer.debug("config.java.schedule.util.CATFaltFileUtil : Source File :"+sourcefile);
	  			Log.customer.debug("config.java.schedule.util.CATFaltFileUtil : Destination path :"+destfile);
	  			File file = new File(sourcefile);
	  			File dir = new File(destfile);

	  			boolean success = file.renameTo(new File(dir, file.getName()));
	  			if (success) {
	  				Log.customer.debug("config.java.schedule.util.CATFaltFileUtil : File was successfully moved");
	  			}
	  			else {
	  				Log.customer.debug("config.java.schedule.util.CATFaltFileUtil : File was not successfully moved");
	  			}

	  			FileWriter fileWriter = new FileWriter(destfile, false);
	  			Log.customer.debug("config.java.schedule.util.CATFaltFileUtil : Destination FileWriter :"+fileWriter);
	  			FileReader fileReader= new FileReader(sourcefile);
	  			Log.customer.debug("config.java.schedule.util.CATFaltFileUtil : Source FileReader :"+fileReader);
	  			if(fileReader == null)
	  			{
	  				Log.customer.debug("config.java.schedule.util.CATFaltFileUtil : Source FileReader is null");
	  				return;
	  			}
	  			if(fileWriter == null)
	  			{
	  				Log.customer.debug("config.java.schedule.util.CATFaltFileUtil : Destination FileReader is null");
	  				return;
	  			}
	  		    BufferedWriter bufferedWriter;
	  		  	bufferedWriter = new BufferedWriter(fileWriter);
	  	        BufferedReader bufferedReader;
	  		 	bufferedReader = new BufferedReader(fileReader);
	  		 	while(bufferedReader.readLine() != null)
	  		 	{
	  		 		 String lineToWrite=bufferedReader.readLine();
	  		         bufferedWriter.write(lineToWrite);
	  		         Log.customer.debug("config.java.schedule.util.CATFaltFileUtil : Written Line:"+ lineToWrite);
	  			     bufferedWriter.newLine();
	  		 	}
	  		 	bufferedReader.close();
	  		 	bufferedWriter.close();



	  		} catch (Exception e) {
	  			e.printStackTrace();
	  			Log.customer
	  					.debug("config.java.schedule.util.CATFaltFileUtil :  Exception in Copy function "
	  							+ e);

	  		}
	}*/
		public  static void copyFile(String sourcefile,String destfile)  {
			try {
				Log.customer.debug("config.java.schedule.util.CATFaltFileUtil : Source File :"+sourcefile);
				Log.customer.debug("config.java.schedule.util.CATFaltFileUtil : Destination path :"+destfile);
				File file = new File(sourcefile);
				File archiveFile = new File(destfile);
				PrintWriter outPW_ArchiveFlatFile = null;
				if (!archiveFile.exists()) {
					Log.customer.debug("config.java.schedule.util.CATFaltFileUtil :File not exist creating file ..");
					archiveFile.createNewFile();
					Log.customer.debug("config.java.schedule.util.CATFaltFileUtil :New file created.");
				}

				outPW_ArchiveFlatFile =new PrintWriter(IOUtil.bufferedOutputStream(archiveFile),true);
				Log.customer.debug("config.java.schedule.util.CATFaltFileUtil :PrintWriter object created.");
				FileReader fileReader= new FileReader(file);
				Log.customer.debug("config.java.schedule.util.CATFaltFileUtil : Source FileReader :"+fileReader);
				BufferedReader bufferedReader;

				bufferedReader = new BufferedReader(fileReader);

				while(bufferedReader.readLine() != null)
				{
					 String lineToWrite=bufferedReader.readLine();
					 outPW_ArchiveFlatFile.write(lineToWrite);
					 Log.customer.debug("config.java.schedule.util.CATFaltFileUtil : Written Line:"+ lineToWrite);
					 outPW_ArchiveFlatFile.println();
				}
				bufferedReader.close();
				outPW_ArchiveFlatFile.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				Log.customer.debug("config.java.schedule.util.CATFaltFileUtil :  Exception in Copy function "+ e);

			}
	}

}
