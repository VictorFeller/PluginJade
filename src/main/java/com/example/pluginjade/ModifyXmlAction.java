package com.example.pluginjade;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import java.util.Arrays;

public class ModifyXmlAction extends AnAction {

    private static final String FILE_NAME = "Jade.xml";
    private static final String RESOURCES_PATH = "src/main/resources/";
    private static final String POPUP_DIALOG_TITLE = "S�lectionnez un environnement";
    private static final String XML_FILE_EXTENSION = ".xml";
    private static final String PATH_TO_CONFIG_FILES = "/JadeConfigsXML/";

    private String pluginHoverText = "No selected env, see Jade.xml";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        // Affiche la bo�te de dialogue avec la liste d�roulante
        ComboBox<String> comboBox = new ComboBox<>(fetchConfigFilesNames());
        int optionPaneChoice = JOptionPane.showConfirmDialog(null, comboBox, POPUP_DIALOG_TITLE, JOptionPane.DEFAULT_OPTION);
        String selectedItem = (String) comboBox.getSelectedItem();

        //Ex�cute le process principal du plugin seulement si on a valid� la popup
        if (optionPaneChoice == JOptionPane.OK_OPTION) {
            String newConfigJadeXml = null;
            try {
                newConfigJadeXml = loadJadeConfigFromSelectedItem(selectedItem);
            } catch (IOException ex) {
                Messages.showErrorDialog(project, "La configuration pour l'environnement \"" + selectedItem + "\" n'a pas �t� trouv�e", "Configuration Introuvable");
            }


            //R�cup�ration du fichier Jade.xml dans le projet WebAVS
            VirtualFile jadeFileXml = findFileInProject(project, FILE_NAME);

            // Appeler la m�thode de modification XML avec l'option s�lectionn�e
            if (jadeFileXml != null && newConfigJadeXml != null) {
                modifyXml(jadeFileXml, newConfigJadeXml);
            }

            //Modifier le contenu du hover sur icon
            pluginHoverText = "Current env : " + selectedItem;
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        //Ajuste le texte hover du plugin en fonction de l'environnement s�lectionn�
        e.getPresentation().setText(pluginHoverText);
    }

    /**
     * R�cup�re tous les noms des environnements pour matcher avec les fichiers xml de configuration
     * @return Tableau de String avec le contenu de l'Enum
     */
    private String[] fetchConfigFilesNames(){
        return Arrays.stream(EnvironnementsJade.values())
                .map(Enum::name)
                .toArray(String[]::new);
    }

    private String loadJadeConfigFromSelectedItem(String selected) throws IOException {
        // Construire le chemin vers le fichier de configuration bas� sur l'option s�lectionn�e
        String configFilePath = PATH_TO_CONFIG_FILES + selected + XML_FILE_EXTENSION;
        // Lire le contenu du fichier de configuration
        InputStream configInputStream = getClass().getResourceAsStream(configFilePath);
        if (configInputStream == null) {
            throw new FileNotFoundException("Fichier de configuration " + configFilePath + " introuvable dans les ressources.");
        }
        byte[] configContentBytes = configInputStream.readAllBytes();

        return new String(configContentBytes, StandardCharsets.UTF_8);
    }

    @Deprecated
    private VirtualFile findFileInProject(Project project, String fileName) {
        // R�cup�ration du fichier Jade.xml dans le projet
        VirtualFile file = project.getBaseDir().findFileByRelativePath(RESOURCES_PATH + fileName);
        if(file != null){
           return file;
        } else {
            Messages.showErrorDialog(project, "Le fichier " + FILE_NAME + " n'a pas �t� trouv� dans " + RESOURCES_PATH, "Fichier Non Trouv�");
            return null;
        }
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


    /**
     * M�thode qui remplace le contenu du fichier Jade.xml par le nouveau contenu
     * @param jadeFileXml
     * @param newConfigJadeXml
     */
    private void modifyXml(VirtualFile jadeFileXml, String newConfigJadeXml) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                // Convertit le contenu XML en tableau d'octets
                byte[] contentBytes = newConfigJadeXml.getBytes(StandardCharsets.UTF_8);

                // Remplace directement le contenu existant par le nouveau contenu XML
                jadeFileXml.setBinaryContent(contentBytes);

                // Rafra�chit le fichier pour refl�ter les modifications dans l'IDE
                VfsUtil.markDirtyAndRefresh(true, true, true, jadeFileXml);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
