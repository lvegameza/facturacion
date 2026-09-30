package pe.com.vegabytes.facturacion;

import io.github.project.openubl.xbuilder.content.models.common.Cliente;
import io.github.project.openubl.xbuilder.content.models.common.Proveedor;
import io.github.project.openubl.xbuilder.content.models.standard.general.DocumentoVentaDetalle;
import io.github.project.openubl.xbuilder.content.models.standard.general.Invoice;
import io.github.project.openubl.xbuilder.enricher.ContentEnricher;
import io.github.project.openubl.xbuilder.enricher.config.DateProvider;
import io.github.project.openubl.xbuilder.enricher.config.Defaults;
import io.github.project.openubl.xbuilder.renderer.TemplateProducer;
import io.github.project.openubl.xsender.models.SunatResponse;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public class Application {

  public static void main(String[] args) throws Exception {

    // =========================================================
    // 1. CREAR FACTURA
    // =========================================================

    Invoice invoice = Invoice.builder().serie("F001").numero(1)

        .proveedor(Proveedor.builder().ruc("20000000001").razonSocial("EMPRESA DEMO SAC").build())

        .cliente(Cliente.builder().nombre("CLIENTE DEMO").numeroDocumentoIdentidad("12121212121")
            .tipoDocumentoIdentidad("6").build())

        .detalle(DocumentoVentaDetalle.builder().descripcion("Producto de prueba")
            .cantidad(new BigDecimal("1")).precio(new BigDecimal("100.00")).unidadMedida("NIU")
            .build())

        .build();


    // =========================================================
    // 2. CONFIGURAR IGV
    // =========================================================

    Defaults defaults = Defaults.builder().igvTasa(new BigDecimal("0.18")).build();


    // =========================================================
    // 3. FECHA DE EMISION
    // =========================================================

    DateProvider dateProvider = () -> LocalDate.now();


    // =========================================================
    // 4. ENRIQUECER FACTURA
    // =========================================================

    ContentEnricher enricher = new ContentEnricher(defaults, dateProvider);

    enricher.enrich(invoice);


    // =========================================================
    // 5. GENERAR XML UBL
    // =========================================================

    String xml = TemplateProducer.getInstance().getInvoice().data(invoice).render();


    // =========================================================
    // 6. FIRMAR XML
    // =========================================================

    XmlSignerService signerService = new XmlSignerService();

    String xmlFirmado = signerService.firmar(xml);

    System.out.println("XML firmado correctamente");

    System.out.println("¿Contiene ds:Signature? " + xmlFirmado.contains("<ds:Signature"));


    // =========================================================
    // 7. GUARDAR XML FIRMADO
    // =========================================================

    String nombreXml = "20000000001-01-F001-1.xml";

    Files.writeString(Path.of(nombreXml), xmlFirmado, StandardCharsets.ISO_8859_1);

    System.out.println("XML firmado guardado: " + nombreXml);


    // =========================================================
    // 8. ENVIAR A SUNAT BETA
    // =========================================================

    SunatSender sender = new SunatSender();

    SunatResponse response =
        sender.enviar(xmlFirmado, nombreXml, "20000000001MODDATOS", "MODDATOS");


    // =========================================================
    // 9. MOSTRAR RESPUESTA
    // =========================================================

    System.out.println();
    System.out.println("====================================");
    System.out.println("RESPUESTA SUNAT");
    System.out.println("====================================");

    System.out.println(response);

    if (response.getSunat() != null && response.getSunat().getCdr() != null) {

      CdrService cdrService = new CdrService();

      String cdrXml = cdrService.extraerXml(response.getSunat().getCdr());

      System.out.println();
      System.out.println("====================================");
      System.out.println("CDR XML");
      System.out.println("====================================");
      System.out.println(cdrXml);
      System.out.println("====================================");
    }
  }
}