package com.example.pluginjade;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


import com.intellij.openapi.ui.ComboBox;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ModifyXmlAction extends AnAction {

    private static final String FILE_NAME = "Jade.xml";
    private static final String RESOURCES_PATH = "src/main/resources/";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Affiche la bo�te de dialogue avec la liste d�roulante
        //TODO VFE construire le tableau de string avec le nom des fichiers xml dans les ressources
        ComboBox<String> comboBox = new ComboBox<>(new String[]{"DEV1", "DEV2", "DEV3"});
        JOptionPane.showMessageDialog(null, comboBox, "Select Modification", JOptionPane.QUESTION_MESSAGE);
        String selected = (String) comboBox.getSelectedItem();

        String configXml = null;
        try {
            configXml = loadConfigFromSelectedItem(selected);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }


        //R�cup�ration du file Jade.xml
        Project project = e.getProject();
        //TODO VFE impl�menter exception
        VirtualFile file = findFileInProject(project, FILE_NAME);

        // Appeler la m�thode de modification XML avec l'option s�lectionn�e
        modifyXml(file, configXml);
    }

    private String loadConfigFromSelectedItem(String selected) throws IOException {
        // Construire le chemin vers le fichier de configuration bas� sur l'option s�lectionn�e
        String configFilePath = "/JadeConfigsXML/" + selected + ".xml";
        // Lire le contenu du fichier de configuration
        InputStream configInputStream = getClass().getResourceAsStream(configFilePath);
        if (configInputStream == null) {
            throw new FileNotFoundException("Fichier de configuration " + configFilePath + " introuvable dans les ressources.");
        }
        byte[] configContentBytes = configInputStream.readAllBytes();

        return new String(configContentBytes, StandardCharsets.UTF_8);
    }

    private VirtualFile findFileInProject(Project project, String fileName) {
        // Impl�mentation pour trouver le fichier dans le projet
        // Cette fonction est un exemple et doit �tre adapt�e � vos besoins
        VirtualFile root = project.getBaseDir();
        return root.findFileByRelativePath(RESOURCES_PATH + fileName);
    }

    private void modifyXmlToAppendContent(VirtualFile file, String option) {
        try {

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file.getInputStream());

            // Normaliser le document XML
            doc.getDocumentElement().normalize();

            // Cr�er un nouvel �l�ment <option>
            Element newOption = doc.createElement("option");
            newOption.appendChild(doc.createTextNode(option));

            // Ajouter cet �l�ment � la racine ou � un �l�ment sp�cifique du document XML
            // Pour cet exemple, nous l'ajoutons � l'�l�ment racine
            doc.getDocumentElement().appendChild(newOption);

            // �crire les modifications dans le fichier XML
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // Pour un formatage joli
            DOMSource source = new DOMSource(doc);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(outputStream);
            transformer.transform(source, result);

            // �crire les modifications retour dans le fichier
            writeToFile(file, outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeToFile(VirtualFile file, byte[] content) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                file.setBinaryContent(content);
                // Rafra�chir le fichier pour refl�ter les modifications dans l'IDE
                VfsUtil.markDirtyAndRefresh(true, true, true, file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    private void modifyXml(VirtualFile file, String newContentXml) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                // Convertit le contenu XML en tableau d'octets
                byte[] contentBytes = newContentXml.getBytes(StandardCharsets.UTF_8);

                // Remplace directement le contenu existant par le nouveau contenu XML
                file.setBinaryContent(contentBytes);

                // Rafra�chit le fichier pour refl�ter les modifications dans l'IDE
                VfsUtil.markDirtyAndRefresh(true, true, true, file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
