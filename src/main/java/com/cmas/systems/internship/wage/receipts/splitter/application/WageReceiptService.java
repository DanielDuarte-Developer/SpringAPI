package com.cmas.systems.internship.wage.receipts.splitter.application;

import com.cmas.systems.internship.wage.receipts.splitter.WageReceiptFileSplitterProperties;
import com.cmas.systems.internship.wage.receipts.splitter.domain.Person;
import com.cmas.systems.internship.wage.receipts.splitter.infrastructure.WageReceiptFileProtector;
import com.cmas.systems.internship.wage.receipts.splitter.infrastructure.WageReceiptFileSplitter;
import com.cmas.systems.internship.wage.receipts.splitter.infrastructure.WageReceiptFileUploader;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

/**
 * @author Nelson Neves ( nelson.neves@cmas-systems.com )
 * @since <next-release>
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class WageReceiptService {

	private final WageReceiptFileSplitterProperties appProperties;

	private final WageReceiptFileUploader fileUploader;

	private final WageReceiptFileProtector receiptFileProtector;

	private final WageReceiptFileSplitter receiptFileSplitter;

	private final ObjectMapper objectMapper;

	@SneakyThrows
	public void processFile( MultipartFile wageReceiptPdf, MultipartFile pwdFile ) {
		Map<String, String> passwordsMap;

		try {
			//Read the Json file of the passwords and save inside a Map
			passwordsMap = objectMapper.readValue( new String( pwdFile.getBytes() ), Map.class );
		}
		catch ( JsonParseException e ) {
			log.error( "Error trying to read JSON file verify if you put the correct file " + e.getMessage() );
			throw new RuntimeException( "Error trying to read JSON file" );
		}
		catch ( MismatchedInputException e ) {
			log.error( "Error {}", e.getMessage() + " Make sure you didn't forget to put a file(s) on the body or if he file is not empty" );
			throw new RuntimeException( "Error trying to get the body file(s)" );
		}

		String randomName = UUID.randomUUID().toString();

		//Create the Temporary files
		File file = createTemporaryFiles( wageReceiptPdf, randomName );

		try ( PDDocument wagesReceipts = PDDocument.load( file ) ) {
			//Create a list of each person (each person is created according to the NIF in the passwords file)
			List<Person> personsList = passwordsMap
				.entrySet()
				.stream()
				.map( entry -> {
					if(entry.getValue() == "" || entry.getKey() == "" || entry.getKey().length() != 9){
						throw new RuntimeException("Incorrect password(s)/nif(s) format");
					}
					return new Person( Integer.parseInt( entry.getKey() ) , entry.getValue() );

				})
				.collect( Collectors.toList() );

			//Splits the pdfs and Checks if it was done any split
			receiptFileSplitter.split( wagesReceipts, personsList );

			//Encrypt the pdf file with the respective person's password
			receiptFileProtector.protectPdfs( personsList, randomName );

			//Upload the files to alfresco
			fileUploader.fileUpload( personsList, randomName );

		}
		catch (NumberFormatException e){
			log.error("Failed to read NIF(s)");
			throw new RuntimeException("Failed to read NIF(s)");
		}
		catch (IOException e){
			log.error("File isn't a valid pdf file");
			throw new RuntimeException("File isn't a valid pdf file");
		}
		catch ( Exception e ) {
			log.error( "Failed Loading file {}", wageReceiptPdf.getOriginalFilename(), e );
			throw new RuntimeException( "Failed Loading file" );
		}
		finally {
			//Delete the Temporary Files
			deleteTemporaryFiles( randomName );
		}
	}

	private File createTemporaryFiles( MultipartFile multipartFilePDF, String randomName ) {
		File randomFolder = new File(appProperties.getTempFolder() + "\\" + randomName);
		randomFolder.mkdir();

		File temporaryFilePDF = new File( randomFolder + "\\" + multipartFilePDF.getOriginalFilename() );

		try ( OutputStream os = Files.newOutputStream( temporaryFilePDF.toPath() ) ) {
			//Write PDF document
			os.write( multipartFilePDF.getBytes() );
			multipartFilePDF.transferTo( temporaryFilePDF );
			return temporaryFilePDF;
		}
		catch ( IOException e ) {
			log.error( "Could not write multipart file inside an empty file, make sure you insert the file in the body." );
			throw new RuntimeException( "Could not write multipart file inside an empty file." );
		}
	}

	public void deleteTemporaryFiles(String randomName) {

		File folder = new File( appProperties.getTempFolder() + "\\" + randomName );
		for ( final File fileEntry : Objects.requireNonNull( folder.listFiles() ) ) {
			try{
				FileUtils.deleteDirectory(folder);
			}
			catch (IOException e){
				log.error( "It Was not possible Delete the files." );
				throw new RuntimeException( "It Was not possible Delete the files." );
			}

			log.info( "Delete " + fileEntry.getName() );
		}
	}
}