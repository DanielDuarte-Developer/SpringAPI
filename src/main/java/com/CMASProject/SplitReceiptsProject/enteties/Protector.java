package com.CMASProject.SplitReceiptsProject.enteties;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.CMASProject.SplitReceiptsProject.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class Protector {

	private final AppProperties appProperties;

	public void protectPdfs(List<Person> personsList){
		for (Person person : personsList) {
			if (person.getDocument() != null)
				protectPersonPdf(person, appProperties.getTempFolder());
		}
	}

	private static void protectPersonPdf(Person person, String destinationPath){
    	try  {
			//Read the PDF files and save on the person
			person.getDocument().save(destinationPath + "\\Rv_"+person.getProcessDate()+" - "+person.getName()+".pdf" );
			PDDocument document = PDDocument.load(new File(destinationPath + "\\Rv_"+person.getProcessDate()+" - "+person.getName()+".pdf" ));
			person.setDocument(document);

			//Encrypt the pdf document
			AccessPermission accessPermission = new AccessPermission();
			StandardProtectionPolicy spp = new StandardProtectionPolicy(person.getPassword(), person.getPassword(),accessPermission);
			spp.setEncryptionKeyLength(256);
			spp.setPermissions(accessPermission);
			person.getDocument().protect(spp);
			String fileName = "Rv_"+person.getProcessDate()+" - "+person.getName()+".pdf";
			person.getDocument().save(destinationPath + "\\"+fileName);
			person.getDocument().close();
			document.close();
			log.info("Created - '"+fileName+"'");
		} catch (IOException e) {
			log.error("It was not possible to protect the pdfs of "+ person.getName()+". Error: "+e.getMessage());
			throw new RuntimeException("It was not possible to protect the pdfs");
		}
	}
}
