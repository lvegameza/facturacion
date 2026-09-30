package pe.com.vegabytes.facturacion;

import io.github.project.openubl.xsender.Constants;
import io.github.project.openubl.xsender.camel.StandaloneCamel;
import io.github.project.openubl.xsender.camel.utils.CamelData;
import io.github.project.openubl.xsender.camel.utils.CamelUtils;
import io.github.project.openubl.xsender.company.CompanyCredentials;
import io.github.project.openubl.xsender.files.ZipFile;
import io.github.project.openubl.xsender.models.SunatResponse;
import io.github.project.openubl.xsender.sunat.BillServiceDestination;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.camel.CamelContext;

public class SunatSender {

  private static final String SUNAT_BETA_URL =
      "https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService";

  public SunatResponse enviar(String xml, String nombreXml, String usuario, String password)
      throws IOException {

    // =========================================================
    // 1. XML -> ZIP
    // =========================================================

    byte[] zipBytes = crearZip(xml, nombreXml);

    String nombreZip = nombreXml.replace(".xml", ".zip");


    // =========================================================
    // 2. Crear ZipFile de OpenUBL
    // =========================================================

    ZipFile zipFile = ZipFile.builder().file(zipBytes).filename(nombreZip).build();


    // =========================================================
    // 3. Credenciales SUNAT
    // =========================================================

    CompanyCredentials credentials =
        CompanyCredentials.builder().username(usuario).password(password).build();


    // =========================================================
    // 4. Configurar SUNAT BETA
    // =========================================================

    BillServiceDestination destination = BillServiceDestination.builder().url(SUNAT_BETA_URL)
        .soapOperation(BillServiceDestination.SoapOperation.SEND_BILL).build();


    // =========================================================
    // 5. Preparar request SOAP
    // =========================================================

    CamelData camelData = CamelUtils.getBillServiceCamelData(zipFile, destination, credentials);


    // =========================================================
    // 6. Inicializar Camel
    // =========================================================

    CamelContext camelContext = StandaloneCamel.getInstance().getMainCamel().getCamelContext();


    // =========================================================
    // 7. ENVIAR A SUNAT
    // =========================================================

    return camelContext.createProducerTemplate()
        .requestBodyAndHeaders(Constants.XSENDER_BILL_SERVICE_URI, camelData.getBody(),
            camelData.getHeaders(), SunatResponse.class);
  }


  // =============================================================
  // XML -> ZIP
  // =============================================================

  private byte[] crearZip(String xml, String nombreXml) throws IOException {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {

      ZipEntry entry = new ZipEntry(nombreXml);

      zipOutputStream.putNextEntry(entry);

      zipOutputStream.write(xml.getBytes(StandardCharsets.ISO_8859_1));

      zipOutputStream.closeEntry();
    }

    return outputStream.toByteArray();
  }
}