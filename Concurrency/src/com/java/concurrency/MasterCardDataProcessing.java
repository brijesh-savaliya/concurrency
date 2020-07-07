package com.java.concurrency;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MasterCardDataProcessing {
	private BlockingQueue<String> workQueue = new ArrayBlockingQueue<>(1000000);
	public static BlockingQueue<CreditCard> creditCardList = new ArrayBlockingQueue<CreditCard>(1000000);
	public static ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
	public static ThreadPoolExecutor dbpool = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
	
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		MasterCardDataProcessing masterCardDataProcessing = new MasterCardDataProcessing();

		for (int i = 0; i < 100; i++) {
			MasterCardDataProcessing.StringProcessing task = masterCardDataProcessing.new StringProcessing();
			pool.execute(task);
		}
		
		masterCardDataProcessing.readTransactions();
		
		
		for (int i = 0; i < 100; i++) {
			MasterCardDataProcessing.DatabaseOperation task = masterCardDataProcessing.new DatabaseOperation();
			dbpool.execute(task);
		}
		
		pool.shutdown();
		
		while(pool.isTerminating()) {
			dbpool.shutdown();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Successfully completed file processing");
		System.out.println("Total time to process 1 million records in sec: " + ((endTime - startTime) / 1000));
		while (!pool.isTerminated() || !dbpool.isTerminated()) {}
	}

	private void readTransactions() {
		File file = new File("C:\\Users\\BriJa\\Documents\\fileio.txt");
		BufferedReader bufferedReader = null;
		String line = "";
		try {
			bufferedReader = new BufferedReader(new FileReader(file));
			while ((line = bufferedReader.readLine()) != null) {
				workQueue.put(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (bufferedReader != null)
					bufferedReader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private class DatabaseOperation implements Runnable {
		ThreadLocal<Connection> threadConnection = new ThreadLocal<Connection>();

		@Override
		public void run() {
			Connection connection = getConnection();
			PreparedStatement preparedStatement = null;
			final String sql = "insert into creditcard (bank,cardno,secretid,name,uniqueid) values (?,?,?,?,?)";

			try {
				Class.forName("com.mysql.jdbc.Driver");
				preparedStatement = connection.prepareStatement(sql);
				int i = 0;
				
				CreditCard creditCard = null;
				while(true) {
					creditCard = MasterCardDataProcessing.creditCardList.poll(10, TimeUnit.SECONDS);
					if(creditCard == null) {
						break;
					}
					i++;
					preparedStatement.setString(1, creditCard.getBank());
					preparedStatement.setString(2, creditCard.getCardno());
					preparedStatement.setString(3, creditCard.getSecretid());
					preparedStatement.setString(4, creditCard.getName());
					preparedStatement.setString(5, creditCard.getUniqueid());
					preparedStatement.addBatch();

					if (i % 10000 == 0) {
						preparedStatement.executeBatch();
						i = 0;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (preparedStatement != null)
						preparedStatement.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					if (connection != null)
						connection.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		private Connection connect()  {
			try {
				final String USER = "root";
				final String PASS = "root";
				String URL = "jdbc:mysql://localhost:3306/development";

				Class.forName("com.mysql.jdbc.Driver");
				Connection connection = DriverManager.getConnection(URL, USER, PASS);
				return connection;
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			return null;

		}

		public Connection getConnection() {
			if (threadConnection.get() == null) {
				Connection connection = connect();
				threadConnection.set(connection);
				return threadConnection.get();
			} else
				return threadConnection.get();
		}

	}

	private class StringProcessing implements Runnable {
		@Override
		public void run() {
			String line;
			try {
				while (true) {
					line = workQueue.poll(2, TimeUnit.SECONDS);
					if (line == null) {
						break;
					}
					StringTokenizer tockens = new StringTokenizer(line, ";");
					StringTokenizer subTocken = null;
					CreditCard creditCard = null;

					while (tockens.hasMoreTokens()) {
						subTocken = new StringTokenizer(tockens.nextToken(), "|");
						while (subTocken.hasMoreTokens()) {
							creditCard = new CreditCard();
							creditCard.setBank(subTocken.nextToken());
							creditCard.setCardno(subTocken.nextToken());
							creditCard.setSecretid(subTocken.nextToken());
							creditCard.setName(subTocken.nextToken());
							creditCard.setUniqueid(subTocken.nextToken());
							creditCardList.put(creditCard);
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class CreditCard {
		private String bank;
		private String cardno;
		private String secretid;
		private String name;
		private String uniqueid;

		public String getBank() {
			return bank;
		}

		public void setBank(String bank) {
			this.bank = bank;
		}

		public String getCardno() {
			return cardno;
		}

		public void setCardno(String cardno) {
			this.cardno = cardno;
		}

		public String getSecretid() {
			return secretid;
		}

		public void setSecretid(String secretid) {
			this.secretid = secretid;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getUniqueid() {
			return uniqueid;
		}

		public void setUniqueid(String uniqueid) {
			this.uniqueid = uniqueid;
		}
	}
}