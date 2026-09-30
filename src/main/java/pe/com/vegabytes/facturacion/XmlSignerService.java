package pe.com.vegabytes.facturacion;

import io.github.project.openubl.xbuilder.signature.CertificateDetails;
import io.github.project.openubl.xbuilder.signature.CertificateDetailsFactory;
import io.github.project.openubl.xbuilder.signature.XMLSigner;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XmlSignerService {

  private static final String KEYSTORE_PATH = "certificado-prueba.jks";

  private static final String KEYSTORE_PASSWORD = "123456";

  private static final String SIGNATURE_ID = "MiEmpresa";


  public String firmar(String xml) throws Exception {

    // =========================================================
    // 1. CARGAR CERTIFICADO
    // =========================================================

    CertificateDetails certificateDetails;

    try (InputStream inputStream = new FileInputStream(KEYSTORE_PATH)) {

      certificateDetails = CertificateDetailsFactory.create(inputStream, KEYSTORE_PASSWORD);
    }


    // =========================================================
    // 2. FIRMAR XML
    // =========================================================

    Document signedDocument =
        XMLSigner.signXML(xml, SIGNATURE_ID, certificateDetails.getX509Certificate(),
            certificateDetails.getPrivateKey());


    System.out.println();
    System.out.println("====================================");
    System.out.println("VALIDACION DE FIRMA EN MEMORIA");
    System.out.println("====================================");


    // =========================================================
    // 3. VALIDAR DOCUMENT ANTES DE SERIALIZAR
    // =========================================================

    boolean validBeforeSerialization =
        validarFirma(signedDocument, certificateDetails.getX509Certificate());

    System.out.println("Firma válida antes de serializar: " + validBeforeSerialization);


    // =========================================================
    // 4. SERIALIZAR
    // =========================================================

    String xmlFirmado = convertirDocumentAString(signedDocument);


    // =========================================================
    // 5. VOLVER A PARSEAR EL XML
    // =========================================================

    Document documentAfterSerialization = convertirStringADocument(xmlFirmado);


    // =========================================================
    // 6. VALIDAR DESPUÉS DE SERIALIZAR
    // =========================================================

    boolean validAfterSerialization =
        validarFirma(documentAfterSerialization, certificateDetails.getX509Certificate());

    System.out.println("Firma válida después de serializar: " + validAfterSerialization);


    System.out.println("====================================");
    System.out.println();


    return xmlFirmado;
  }


  // =============================================================
  // VALIDAR FIRMA XMLDSIG
  // =============================================================

  private boolean validarFirma(Document document, X509Certificate certificate) throws Exception {

    XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");


    // Buscar ds:Signature

    Node signatureNode = document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature").item(0);


    if (signatureNode == null) {

      System.out.println("No se encontró ds:Signature");

      return false;
    }


    // Contexto de validación

    DOMValidateContext validateContext =
        new DOMValidateContext(certificate.getPublicKey(), signatureNode);


    // Para este POC permitimos SHA1
    // porque OpenUBL 5.1.1 genera RSA-SHA1/SHA1.

    validateContext.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE);


    // Obtener firma

    XMLSignature signature = factory.unmarshalXMLSignature(validateContext);


    // =========================================================
    // VALIDAR CADA REFERENCE
    // =========================================================

    boolean referencesValid = true;

    for (Object object : signature.getSignedInfo().getReferences()) {

      Reference reference = (Reference) object;

      boolean valid = reference.validate(validateContext);

      System.out.println("Reference URI=" + reference.getURI() + " válida: " + valid);

      System.out.println("Digest calculado/validado: " + reference.getDigestValue());

      referencesValid = referencesValid && valid;
    }


    // =========================================================
    // VALIDAR FIRMA COMPLETA
    // =========================================================

    boolean signatureValid = signature.validate(validateContext);


    System.out.println("Firma criptográfica válida: " + signatureValid);


    return referencesValid && signatureValid;
  }


  // =============================================================
  // DOCUMENT -> STRING
  // =============================================================

  private String convertirDocumentAString(Document document) throws Exception {

    TransformerFactory transformerFactory = TransformerFactory.newInstance();

    Transformer transformer = transformerFactory.newTransformer();

    transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");

    transformer.setOutputProperty(OutputKeys.INDENT, "no");


    StringWriter writer = new StringWriter();


    transformer.transform(new DOMSource(document), new StreamResult(writer));


    return writer.toString();
  }


  // =============================================================
  // STRING -> DOCUMENT
  // =============================================================

  private Document convertirStringADocument(String xml) throws Exception {

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    factory.setNamespaceAware(true);


    return factory.newDocumentBuilder()
        .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.ISO_8859_1)));
  }
}