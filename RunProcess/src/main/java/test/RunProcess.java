package test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.File;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;

public class RunProcess
{
	private static List<String> filesListInDir = new ArrayList<>();
	
	private static boolean existingWork = false;
	private static String autoWorkFileId = "";
	
	private static Map<String, String> autoWorks = new HashMap<>();
	
	public static void main(String[] args) 
	{		
		NetHttpTransport HTTP_TRANSPORT;
		Drive service = null;
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			service = new Drive.Builder(HTTP_TRANSPORT, GDriveConfig.JSON_FACTORY, GDriveConfig.getCredentials(HTTP_TRANSPORT))
	                .setApplicationName(GDriveConfig.APPLICATION_NAME)
	                .build();
		} catch (GeneralSecurityException | IOException e1) 
		{
			System.out.println("Could not connect to google drive");
			System.exit(0);
		}
		
        java.io.File temp = null;
		
		Runtime.getRuntime().addShutdownHook(new Thread() 
		{
		    @Override
		    public void run() 
		    {
		        System.out.println("Terminating the Process...");

		        try {
		        	Thread.sleep(1000);
					Runtime.getRuntime().exec("taskkill /f /im cmd.exe") ;
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
					System.out.println("Something went Wrong...Exiting");
					System.exit(0);
				}
		    }
		});
		
		Scanner sc = new Scanner(System.in);
		String sourcePath = "", workName = "";
		System.out.println("Enter the path of source directory(For eg - E:/MyFiles/Folder)");
		try {
			sourcePath = sc.nextLine();
			while(sourcePath.equals("") || !new java.io.File(sourcePath).exists())
			{
				System.out.println("Invalid Source Path");
				System.out.println("Please Enter the correct path of source directory");
				sourcePath = sc.nextLine();
			}
		}
		catch(NoSuchElementException e)
		{
			System.out.println("Process Stopped by User");
			System.exit(0);
		}

		try {
			java.io.File myObj = new java.io.File("tokens/fileId.txt");
			if(!myObj.exists())
			{
	        	myObj.createNewFile();
				workName = askForName(sc);
			}
			else
			{
	        	Scanner myReader = new Scanner(myObj);
	            while (myReader.hasNextLine()) {
	            	String line = myReader.nextLine();
	            	autoWorks.put(line.split(" ")[0],line.split(" ")[1]);
	            }
	            myReader.close();
	            workName = ifNotNewWork(sc);
			}
			
			System.out.println("Starting Process...");
			java.io.File source = new java.io.File(sourcePath);
			Path srcPath = Paths.get(sourcePath);
			
			String src = sourcePath.substring(0,sourcePath.lastIndexOf("/"));
			String tempPath = src+"/TempFiles";
			temp = new java.io.File(tempPath);
			temp.mkdir();
			
			Thread.sleep(1000);
			
			Path path = Paths.get(tempPath);
			Files.setAttribute(path, "dos:hidden", true);
			
			System.out.println("Process Running...Press (ctrl+C) to stop ");
			Thread.sleep(500);
			System.out.println("A temporary backup will always gets created at "+tempPath);
			
			String fileName = "AutoFiles_"+workName+"_"+new SimpleDateFormat("dd-MM-yyyy").format(new Date())+".zip";
			String zipDirName = tempPath+"/"+fileName;
			
		    do
			{

				WatchService watcher = srcPath.getFileSystem().newWatchService();

				srcPath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
				WatchKey watchKey = watcher.take();
				List<WatchEvent<?>> events = watchKey.pollEvents();
				for (WatchEvent<?> event : events) 
				{
					if(event.kind() == StandardWatchEventKinds.ENTRY_CREATE)
						System.out.println("Created "+event.context());
					if(event.kind() == StandardWatchEventKinds.ENTRY_MODIFY)
						System.out.println("Modified "+event.context());
					if(event.kind() == StandardWatchEventKinds.ENTRY_DELETE)
						System.out.println("Deleted "+event.context());
				}
				
				java.io.File zipFile = new java.io.File(zipDirName);
				zipFile.delete();
				
				filesListInDir.clear();
				
				zipDirectory(source,zipDirName);
				
				File fileMetadata = new File();
		        fileMetadata.setName(fileName);
		        java.io.File filePath = new java.io.File(zipDirName);
		        
		        
		        FileContent mediaContent = new FileContent("application/zip", filePath);
		        if(existingWork == false && autoWorkFileId.equals("")) 
		        {
			        File file2 = service.files().create(fileMetadata, mediaContent)
			            .setFields("id")
			            .execute();
			        
			        autoWorkFileId = file2.getId();
			        FileWriter myWriter = new FileWriter(myObj);
			        myWriter.write(workName+" "+file2.getId());
			        myWriter.close();
		        }
		        else
		        {
		        	service.files().update(autoWorkFileId, fileMetadata, mediaContent).execute();
		        }
		        System.out.println("Changes Uploaded");
		        watchKey.reset();
				Thread.sleep(1000);
			}
			while(true);
		}
		catch (IOException | InterruptedException e) {
			e.printStackTrace();
			System.out.println("Something went Wrong...Exiting");
			System.exit(0);
		}
		finally {
			sc.close();
		}
	}
	
    private static void zipDirectory(java.io.File dir, String zipDirName) {
        try {
            populateFilesList(dir);

            FileOutputStream fos = new FileOutputStream(zipDirName);
            
            ZipOutputStream zos = new ZipOutputStream(fos);
            for(String filePath : filesListInDir){
  
                ZipEntry ze = new ZipEntry(filePath.substring(dir.getAbsolutePath().length()+1, filePath.length()));
                zos.putNextEntry(ze);

                FileInputStream fis = new FileInputStream(filePath);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
                fis.close();
            }
            zos.close();
            fos.close();
        } catch (IOException e) 
        {
        	e.printStackTrace();
			System.out.println("Something went Wrong...Exiting");
			System.exit(0);
        }
    }

    private static void populateFilesList(java.io.File dir) throws IOException {
    	java.io.File[] files = dir.listFiles();
        for(java.io.File file : files){
            if(file.isFile()) filesListInDir.add(file.getAbsolutePath());
            else populateFilesList(file);
        }
    }
    
    private static String askForName(Scanner sc) {
    	System.out.println("Enter New AutoWork Name");
		
		String workName = sc.nextLine();
		while(workName.equals(""))
		{
			System.out.println("Invalid data received");
			System.out.println("Please Enter the valid name");
			workName = sc.nextLine();
		}
		return workName;
    }
    
    private static String ifNotNewWork(Scanner sc) 
    {
    	String workName = "";
		System.out.println("Enter the option : \n1. New AutoWork \n"
						  +"2. Existing AutoWork");
		String ans = sc.nextLine();
		work : while(workName=="")
		{
			if(ans.equals("1"))
				workName = askForName(sc);
			else if(ans.equals("2"))
			{		
				System.out.println("Enter the AutoWork Name");
				String ans2 = sc.nextLine();
				while(ans2 == "")
				{
					System.out.println("Invalid input recieved");												
					System.out.println("Enter the AutoWork name");
					ans2 = sc.nextLine();							
				}
				if(!autoWorks.keySet().contains(ans2))
				{
					System.out.println("AutoWork does not exist");	
					System.out.println("1. Try Again \n"						
							  +"2. Cancel");
					String ans3 = sc.nextLine();
					while(workName == "")
					{
						if(ans3.equals("2"))
							ifNotNewWork(sc);
						else if(ans3.equals("1"))
							continue work;
						else
						{
							System.out.println("Invalid input recieved");												
							System.out.println("1. Try Again \n"						
									  +"2. Cancel");
							ans3 = sc.nextLine();																
						}
					}
				}
				else
				{
					workName = ans2;
					existingWork = true;
					autoWorkFileId = autoWorks.get(workName);
				}
			}
			else
			{
				System.out.println("Invalid input recieved");												
				System.out.println("Enter the option :\n1. New AutoWork \n"
								+  "2. Existing AutoWork");
				ans = sc.nextLine();				
			}
		}
		return workName;
    }
}
