package pe.com.vegabytes.facturacion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CdrService {

  public String extraerXml(byte[] cdrZip) throws IOException {

    try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(cdrZip))) {

      ZipEntry entry;

      while ((entry = zipInputStream.getNextEntry()) != null) {

        System.out.println("Entrada encontrada en CDR: " + entry.getName());

        // Ignoramos directorios como dummy/
        if (entry.isDirectory()) {
          continue;
        }

        // Buscamos el XML del CDR
        if (entry.getName().toLowerCase().endsWith(".xml")) {

          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

          byte[] buffer = new byte[1024];
          int bytesRead;

          while ((bytesRead = zipInputStream.read(buffer)) != -1) {

            outputStream.write(buffer, 0, bytesRead);
          }

          return outputStream.toString(StandardCharsets.UTF_8);
        }
      }
    }

    throw new IOException("No se encontró el XML dentro del CDR ZIP");
  }
}