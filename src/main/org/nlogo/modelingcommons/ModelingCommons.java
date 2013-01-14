package org.nlogo.modelingcommons;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.nlogo.app.App;
import org.nlogo.app.ModelSaver;
import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.nvm.Procedure;
import org.nlogo.swing.MessageDialog;
import org.nlogo.swing.ModalProgressTask;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Ben
 * Date: 10/25/12
 * Time: 1:19 AM
 * To change this template use File | Settings | File Templates.
 */



public class ModelingCommons {
  private static final String HOST = "http://localhost:3000";
  //private static final String HOST = "http://modelingcommons.org";
  private static HttpClient http = new DefaultHttpClient();
  private JSONParser json = new JSONParser();
  private static Person person = null;
  private String uploadedModelURL;
  private String uploadedModelName;
  private String newUserAgreement;
  private List<String> priorityCountries;
  private List<String> unpriorityCountries;

  private ModelSaver modelSaver;
  private Frame frame;
  private App app;
  private List<Group> groups = null;

  public ModelingCommons(ModelSaver modelSaver, Frame frame, App app) {
    this.modelSaver = modelSaver;
    this.frame = frame;
    this.app = app;
  }
  public static enum Sex {
    MALE("m"),
    FEMALE("f");

    private String str;
    private Sex(String str) {
      this.str = str;
    }
    public String toString() {
      return str;
    }
  }

  public static class Month {
    private int monthNum;
    private String monthString;

    private static List<Month> months;

    private Month(int monthNum, String monthString) {
      this.monthNum = monthNum;
      this.monthString = monthString;
    }

    public int getMonthNum() {
      return monthNum;
    }

    public String toString() {
      return monthString;
    }

    static {
      months = new ArrayList<Month>(12);
      String[] monthNames = DateFormatSymbols.getInstance().getMonths();
      for(int i = 0; i < monthNames.length; i++) {
        if(monthNames[i].length() > 0) {
          months.add(i, new Month(i + 1, monthNames[i]));
        }
      }
    }

    public static List<Month> getMonths() {
      return months;
    }
  }

  public class Person {
    private String firstName;
    private String lastName;
    private int id;
    private String avatarURL;
    private String emailAddress;

    public Person(String firstName, String lastName, int id, String avatarURL, String emailAddress) {
      this.firstName = firstName;
      this.lastName = lastName;
      this.id = id;
      this.avatarURL = avatarURL;
      this.emailAddress = emailAddress;
    }

    public String getFirstName() {
      return firstName;
    }

    public String getLastName() {
      return lastName;
    }

    public int getId() {
      return id;
    }

    public String getAvatarURL() {
      return avatarURL;
    }

    public String getEmailAddress() {
      return emailAddress;
    }
  }

  public class Group {
    private int id;
    private String name;
    public Group(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String toString() {
      return getName();
    }
  }
  public static class Permission {
    private String id;
    private String name;
    public Permission(String id, String name) {
      this.id = id;
      this.name = name;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }
    public String toString() {
      return getName();
    }

    private static Map<String, Permission> permissions;

    static {
      permissions = new HashMap(3);
      permissions.put("a", new Permission("a", "everyone"));
      permissions.put("g", new Permission("g", "group members only"));
      permissions.put("u", new Permission("u", "you only"));
    }
    public static Map<String, Permission> getPermissions() {
      return permissions;
    }
  }
  public abstract class Image {
    public abstract BufferedImage getImage() throws ImageException;
  }

  public class CurrentModelViewImage extends Image {
    public CurrentModelViewImage() {}

    @Override
    public BufferedImage getImage() throws ImageException {
      return app.workspace().exportView();
    }
  }

  public class FileImage extends Image {
    private String filePath;
    public FileImage(String filePath) {
      this.filePath = filePath;
    }

    @Override
    public BufferedImage getImage() throws ImageException {
      if(filePath == null || filePath.length() == 0) {
        throw new ImageException("Image path cannot be blank");
      }
      File file = new File(filePath);
      BufferedImage image = null;
      try {
        image = ImageIO.read(file);
      } catch(IOException e) {
        throw new ImageException("Invalid image file");
      }
      return image;
    }
  }

  public class AutoGeneratedModelImage extends Image {
    public AutoGeneratedModelImage() {

    }
    //Based on org.nlogo.prim.etc._makepreview
    @Override
    public BufferedImage getImage() throws ImageException {
      Map<String, Procedure> map = app.workspace().getProcedures();
      try {
        HeadlessWorkspace headless = HeadlessWorkspace.newInstance();
        headless.openString(modelSaver.save());

        /*Procedure procedure =
           headless.compileForRun
               ("random-seed 0 " + headless.previewCommands() +
                   "\nprint \"GENERATED: " + headless + "\"",
                   context, false);
       JobOwner jobOwner = new SimpleJobOwner("Modeling Commons Preview Image", headless.mainRNG(), Observer.class);
       headless.runCompiledCommands(jobOwner, procedure);*/
        String command = "random-seed 0 " + headless.previewCommands();
        System.out.println("Autogeneration command: " + command);
        headless.command(command);
        BufferedImage image = headless.exportView();
        headless.dispose();
        return image;
      } catch(InterruptedException e) {
        System.out.println("Interrupted exception " + e.getMessage());
      } catch (Exception e) {
        throw new ImageException("Could not autogenerate preview image: " + e.getMessage());

      }
      return null;
    }
  }

  String login(String email, String password) {
    System.out.println("Logging in");
    System.out.println("Path: " + app.workspace().getModelPath());
    try {

      HttpPost post = new HttpPost(HOST + "/account/login_action");
      post.addHeader("Accept", "application/json");
      List<NameValuePair> credentials = new ArrayList<NameValuePair>(2);
      credentials.add(new BasicNameValuePair("email_address", email));
      credentials.add(new BasicNameValuePair("password", password));
      UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(credentials, Consts.UTF_8);
      post.setEntity(formEntity);
      HttpResponse response =  http.execute(post);
      HttpEntity entity = response.getEntity();
      String responseStr = EntityUtils.toString(response.getEntity());
      System.out.println(responseStr);
      EntityUtils.consume(entity);
      try {
        JSONObject obj = (JSONObject)(json.parse(responseStr));
        String status = (String)(obj.get("status"));
        System.out.println("Status: " + status);
        if(status.equals("SUCCESS")) {
          JSONObject personObj = (JSONObject)(obj.get("person"));
          person = new Person(
              (String)(personObj.get("first_name")),
              (String)(personObj.get("last_name")),
              ((Number)(personObj.get("id"))).intValue(),
              (String)(personObj.get("avatar")),
              (String)(personObj.get("email_address"))
          );
        }

        return status;
      } catch(ParseException e) {
        return "INVALID_RESPONSE_FROM_SERVER";
      }


    } catch(IOException e) {
      return "CONNECTION_ERROR";
    }
  }
  String logout() {
    System.out.println("Logging out");
    try {
      HttpPost post = new HttpPost(HOST + "/account/logout");
      post.addHeader("Accept", "application/json");

      HttpResponse response =  http.execute(post);
      HttpEntity entity = response.getEntity();
      String responseStr = EntityUtils.toString(response.getEntity());
      EntityUtils.consume(entity);
      try {
        JSONObject obj = (JSONObject)(json.parse(responseStr));
        String status = (String)(obj.get("status"));
        System.out.println("Status: " + status);
        if(status.equals("SUCCESS") || status.equals("NOT_LOGGED_IN")) {
          person = null;
        }
        return status;
      } catch(ParseException e) {
        return "INVALID_RESPONSE_FROM_SERVER";
      }


    } catch(IOException e) {
      return "CONNECTION_ERROR";
    }
  }
  String createUser(final String firstName, final String lastName, final String emailAddress, final Sex sex, final String country, final Integer birthdayYear, final Month birthdayMonth, final Integer birthdayDay, final String password, final Image profilePicture) {
    try {

      HttpPost post = new HttpPost(HOST + "/account/create");
      post.addHeader("Accept", "application/json");
      MultipartEntity requestEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
      requestEntity.addPart("new_person[first_name]", new StringBody(firstName, "text/plain", Consts.UTF_8));
      requestEntity.addPart("new_person[last_name]", new StringBody(lastName, "text/plain", Consts.UTF_8));
      requestEntity.addPart("new_person[email_address]", new StringBody(emailAddress, "text/plain", Consts.UTF_8));
      requestEntity.addPart("new_person[sex]", new StringBody(sex.toString(), "text/plain", Consts.UTF_8));
      requestEntity.addPart("new_person[country_name]", new StringBody(country, "text/plain", Consts.UTF_8));
      String birthdayYearString = "";
      if(birthdayYear != null) {
        birthdayYearString = birthdayYear.toString();
      }
      String birthdayMonthString = "";
      if(birthdayMonth != null) {
        birthdayMonthString = "" + birthdayMonth.getMonthNum();
      }
      String birthdayDayString = "";
      if(birthdayDay != null) {
        birthdayDayString = birthdayDay.toString();
      }
      requestEntity.addPart("new_person[birthdate(1i)]", new StringBody(birthdayYearString, "text/plain", Consts.UTF_8));
      requestEntity.addPart("new_person[birthdate(2i)]", new StringBody(birthdayMonthString, "text/plain", Consts.UTF_8));
      requestEntity.addPart("new_person[birthdate(3i)]", new StringBody(birthdayDayString, "text/plain", Consts.UTF_8));
      requestEntity.addPart("new_person[password]", new StringBody(password, "text/plain", Consts.UTF_8));
      if(profilePicture != null) {
        try {
          ByteArrayOutputStream profilePictureStream = new ByteArrayOutputStream();
          BufferedImage image = profilePicture.getImage();
          if(image == null) {
            return "INVALID_PROFILE_PICTURE";
          }
          ImageIO.write(image, "png", profilePictureStream);
          requestEntity.addPart("new_person[avatar]", new ByteArrayBody(profilePictureStream.toByteArray(), firstName + "_" + lastName + ".png"));
        } catch(ImageException e) {
          return "INVALID_PROFILE_PICTURE";
        }
      }
      requestEntity.addPart("new_person[registration_consent]", new StringBody("1", "text/plain", Consts.UTF_8));
      post.setEntity(requestEntity);

      HttpResponse response =  http.execute(post);
      HttpEntity entity = response.getEntity();
      String responseStr = EntityUtils.toString(response.getEntity());
      System.out.println(responseStr);
      EntityUtils.consume(entity);
      try {
        JSONObject obj = (JSONObject)(json.parse(responseStr));
        String status = (String)(obj.get("status"));
        System.out.println("Status: " + status);
        if(status.equals("SUCCESS")) {
          JSONObject personObj = (JSONObject)(obj.get("person"));
          person = new Person(
            (String)(personObj.get("first_name")),
            (String)(personObj.get("last_name")),
            ((Number)(personObj.get("id"))).intValue(),
            (String)(personObj.get("avatar")),
            (String)(personObj.get("email_address"))
          );
        }
        return status;
      } catch(ParseException e) {
        return "INVALID_RESPONSE_FROM_SERVER";
      }


    } catch(IOException e) {
      return "CONNECTION_ERROR";
    }
  }
  String uploadModel(final String modelName, final Group group, final Permission visibility, final Permission changeability, final Image previewImage) {
    System.out.println("uploading model");

    try {

      HttpPost post = new HttpPost(HOST + "/upload/create_model");
      post.addHeader("Accept", "application/json");
      MultipartEntity requestEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
      requestEntity.addPart("new_model[name]", new StringBody(modelName, "text/plain", Consts.UTF_8));
      requestEntity.addPart("read_permission", new StringBody(visibility.getId().toString(), "text/plain", Consts.UTF_8));
      requestEntity.addPart("write_permission", new StringBody(changeability.getId().toString(), "text/plain", Consts.UTF_8));
      if(group != null) {
        requestEntity.addPart("group_id", new StringBody("" + group.getId(), "text/plain", Consts.UTF_8));
      }
      requestEntity.addPart("new_model[uploaded_body]", new StringBody(modelSaver.save(), "text/plain", Consts.UTF_8) {
        public String getFilename() {
          return modelName + ".nlogo";
        }
      });
      if(previewImage != null) {
        try {
          ByteArrayOutputStream previewImageStream = new ByteArrayOutputStream();
          BufferedImage image = previewImage.getImage();
          if(image == null) {
            return "INVALID_PREVIEW_IMAGE";
          }
          ImageIO.write(image, "png", previewImageStream);
          requestEntity.addPart("new_model[uploaded_preview]", new ByteArrayBody(previewImageStream.toByteArray(), modelName + ".png"));
        } catch(ImageException e) {
          return "INVALID_PREVIEW_IMAGE";
        }
      }

      post.setEntity(requestEntity);

      HttpResponse response =  http.execute(post);
      HttpEntity entity = response.getEntity();
      String responseStr = EntityUtils.toString(response.getEntity());
      System.out.println(responseStr);
      EntityUtils.consume(entity);
      try {
        JSONObject obj = (JSONObject)(json.parse(responseStr));
        String status = (String)(obj.get("status"));
        System.out.println("Status: " + status);
        if(status.equals("SUCCESS")) {
          JSONObject model = ((JSONObject)(obj.get("model")));
          this.uploadedModelURL = (String)(model.get("url"));
          this.uploadedModelName = (String)(model.get("name"));
        }
        return status;
      } catch(ParseException e) {
        return "INVALID_RESPONSE_FROM_SERVER";
      }


    } catch(IOException e) {
      return "CONNECTION_ERROR";
    }
  }
  void downloadGroups() throws ParseException, IOException {
    List<Group> newGroups = new ArrayList<Group>();
    HttpGet get = new HttpGet(HOST + "/account/list_groups");
    get.addHeader("Accept", "application/json");
    HttpResponse response =  http.execute(get);
    HttpEntity entity = response.getEntity();
    String responseStr = EntityUtils.toString(response.getEntity());
    EntityUtils.consume(entity);
    JSONObject obj = (JSONObject)(json.parse(responseStr));
    JSONArray groups = (JSONArray)(obj.get("groups"));
    Iterator<JSONObject> iterator = groups.iterator();
    while(iterator.hasNext()) {
      JSONObject group = iterator.next();
      int id = ((Number)(group.get("id"))).intValue();
      String name = (String)(group.get("name"));
      newGroups.add(new Group(id, name));
    }
    this.groups = newGroups;
  }

  void downloadNewUserParameters() throws ParseException, IOException{
    List<String> priorityCountries = new ArrayList<String>();
    List<String> unpriorityCountries = new ArrayList<String>();
    HttpGet get = new HttpGet(HOST + "/account/new");
    get.addHeader("Accept", "application/json");
    HttpResponse response =  http.execute(get);
    HttpEntity entity = response.getEntity();
    String responseStr = EntityUtils.toString(response.getEntity());
    EntityUtils.consume(entity);
    JSONObject obj = (JSONObject)(json.parse(responseStr));
    JSONArray countries = (JSONArray)(obj.get("countries"));
    Iterator<JSONObject> iterator = countries.iterator();
    while(iterator.hasNext()) {
      JSONObject country = iterator.next();
      String countryName = (String)(country.get("name"));
      Boolean isPriority = (Boolean)(country.get("priority"));
      if(isPriority) {
        priorityCountries.add(countryName);
      } else {
        unpriorityCountries.add(countryName);
      }
    }
    String userAgreement = (String)(obj.get("user_agreement"));
    this.newUserAgreement = userAgreement;
    this.unpriorityCountries = unpriorityCountries;
    this.priorityCountries = priorityCountries;
  }

  public boolean isLoggedIn() {
    return person != null;
  }

  public String getUploadedModelURL() {
    return uploadedModelURL;
  }

  public String getUploadedModelName() {
    return uploadedModelName;
  }

  public List<Group> getGroups() {
    return groups;
  }

  public List<String> getPriorityCountries() {
    return priorityCountries;
  }

  public List<String> getUnpriorityCountries() {
    return unpriorityCountries;
  }

  public String getNewUserAgreement() {
    return newUserAgreement;
  }

  public Person getPerson() {
    return person;
  }


  public void promptForLogin(final String error) {
    JDialog loginDialog = new ModelingCommonsLoginDialog(frame, this, error);
    loginDialog.setVisible(true);
  }
  public void promptForLogin() {
    promptForLogin(" ");
  }
  public void promptForUpload(final String error) {
    ModalProgressTask.apply(frame, "Loading groups you belong to", new Runnable() {
      @Override
      public void run() {
        try {
          downloadGroups();
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              boolean enableAutoGeneratePreviewImage = app.workspace().getProcedures().get("SETUP") != null && app.workspace().getProcedures().get("GO") != null;
              JDialog uploadDialog = new ModelingCommonsUploadDialog(frame, ModelingCommons.this, error, enableAutoGeneratePreviewImage);
              uploadDialog.setVisible(true);
            }
          });
        } catch (IOException e) {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              MessageDialog.show("Error connecting to Modeling Commons", "Could not connect to Modeling Commons");
            }
          });

        } catch (ParseException e) {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              MessageDialog.show("Error connecting to Modeling Commons", "Invalid response from Modeling Commons");
            }
          });

        }

      }
    });
  }
  public void promptForUpload() {
    promptForUpload(" ");
  }
  public void promptForSuccess(final String error) {
    JDialog successDialog = new ModelingCommonsUploadSuccessDialog(frame, this, error);
    successDialog.setVisible(true);
  }
  public void promptForSuccess() {
    promptForSuccess(" ");
  }
  public void promptForCreateAccount(final String error) {
    ModalProgressTask.apply(frame, "Loading new user information", new Runnable() {
      @Override
      public void run() {
        try {
          downloadNewUserParameters();
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              JDialog createAccountDialog = new ModelingCommonsNewUserDialog(frame, ModelingCommons.this, error);
              createAccountDialog.setVisible(true);
            }
          });
        } catch (IOException e) {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              MessageDialog.show("Error connecting to Modeling Commons", "Could not connect to Modeling Commons");
            }
          });

        } catch (ParseException e) {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              MessageDialog.show("Error connecting to Modeling Commons", "Invalid response from Modeling Commons");
            }
          });

        }

      }
    });

  }
  public void promptForCreateAccount() {
    promptForCreateAccount(" ");
  }
  public void saveToModelingCommons() {
    if(!isLoggedIn()) {
      promptForLogin();
    } else {
      promptForUpload();
    }

  }
}
