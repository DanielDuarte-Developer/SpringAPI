package com.CMASProject.SplitReceiptsProject.enteties;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

public class Protector {
    
	public static void protectPersonPdf(Person person, String destinationPath){
    	try  {
			//Read the PDF files and save on the person
			person.getDocument().save(destinationPath + "\\Rv_"+person.getProcessDate()+" - "+person.getName()+".pdf" );
			PDDocument document = PDDocument.load(new File(destinationPath + "\\Rv_"+person.getProcessDate()+" - "+person.getName()+".pdf" ));
			person.setDocument(document);

			//Encrypt the pdf document
			AccessPermission accessPermission = new AccessPermission();
												//TODO Replace the ownerPassword for the person Password
			StandardProtectionPolicy spp = new StandardProtectionPolicy("12345", person.getPassword(),accessPermission);
			spp.setEncryptionKeyLength(256);
			spp.setPermissions(accessPermission);
			person.getDocument().protect(spp);
			person.getDocument().save(destinationPath + "\\Rv_"+person.getProcessDate()+" - "+person.getName()+".pdf");
			person.getDocument().close();
			document.close();

		} catch (IOException e) {
			System.out.println("It was not possible to protect the pdfs. Error: "+e.getMessage()+"\nExiting program.");
			Runtime.getRuntime().exit(6);
		}
	}
}