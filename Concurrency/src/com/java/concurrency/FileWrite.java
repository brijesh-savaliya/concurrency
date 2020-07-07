package com.java.concurrency;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileWrite {
	public static void main(String[] args) {
		File file = new File("C:\\Users\\BriJa\\Documents\\fileio.txt");
		BufferedWriter bufferedWriter = null;
		String content = "Bank Of America|C128944|45852|Michel Tofel|ABJ58952;";
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(file));
			if(!file.exists()) {
				file.createNewFile();
			}
			
			for(int i=1;i<=1000000;i++) {
				if(i % 5 == 0) {
					bufferedWriter.newLine();
				}else {
					bufferedWriter.write(content);
				}
			}
			
			System.out.println("File Write operation completed successfully.");
		}catch(IOException e) {
			e.printStackTrace();
		}catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if(bufferedWriter != null)
					bufferedWriter.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}		
	}
}