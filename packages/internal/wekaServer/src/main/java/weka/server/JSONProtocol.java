/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    JSONProtocol.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server;

import org.apache.commons.codec.binary.Base64;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import weka.core.WekaException;
import weka.experiment.TaskStatusInfo;
import weka.server.knowledgeFlow.ScheduledNamedKnowledgeFlowTask;
import weka.server.knowledgeFlow.UnscheduledNamedKnowledgeFlowTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Routines for encoding/decoding data to/from JSON
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class JSONProtocol {

  public static final String JSON_MIME_TYPE = "application/json";
  public static final String CHARACTER_ENCODING = "UTF-8";

  public static final String JSON_CLIENT_KEY = "clientJ";

  public static final String RESPONSE_TYPE_KEY = "response";
  public static final String RESPONSE_MESSAGE_KEY = "message";
  public static final String RESPONSE_PAYLOAD_KEY = "payload";

  public static final String PAYLOAD_TYPE_KEY = "payloadType";

  // TASK stuff
  public static final String TASK_NAME_KEY = "taskName";
  public static final String TASK_TYPE_KEY = "taskType";

  public static final String TASK_TYPE_KF = "knowledgeFlow";
  public static final String TASK_TYPE_EXPLORER_CLASSIFIER_CV = "classifierCV";

  // Schedule stuff
  public static final String SCHEDULE_DATE_FORMAT_KEY = "dateFormat";
  public static final String SCHEDULE_START_DATE_KEY = "startDate";
  public static final String SCHEDULE_END_DATE_KEY = "endDate";
  public static final String SCHEDULE_REPEAT_UNIT_KEY = "reapeatUnit";
  public static final String SCHEDULE_REPEAT_VALUE_KEY = "repeatValue";
  public static final String SCHEDULE_DAY_OF_WEEK_KEY = "dayOfWeek";
  public static final String SCHEDULE_MONTH_OF_YEAR_KEY = "monthOfYear";
  public static final String SCHEDULE_DAY_OF_MONTH_KEY = "dayOfMonth";
  public static final String SCHEDULE_OCCURRENCE_KEY = "occurrence";

  // TaskStatusInfo stuff
  public static final String TASK_STATUS_INFO_EXECUTION_STATUS_KEY =
    "executionStatus";
  public static final String TASK_STATUS_INFO_STATUS_MESSAGE_KEY =
    "statusMessage";
  public static final String TASK_STATUS_INFO_TASK_RESULT_KEY = "taskResult";

  // KF task stuff
  public static final String KF_FLOW_PAYLOAD_ID = "kfFlow";
  public static final String KF_SEQUENTIAL_PROPERTY = "kfSequential";
  public static final String ENV_PAYLOAD_ID = "envVars";

  // Schedule
  public static final String SCHEDULE_PAYLOAD_ID = "schedule";

  /**
   * Creates pre-configured error response
   *
   * @param errorMessage the error message to include in the response
   * @return the response
   */
  public static Map<String, Object> createErrorResponseMap(String errorMessage) {
    Map<String, Object> response = new HashMap<String, Object>();
    response.put(RESPONSE_TYPE_KEY, WekaServlet.RESPONSE_ERROR);
    response.put(RESPONSE_MESSAGE_KEY, errorMessage);

    return response;
  }

  /**
   * Creates a pre-configured OK response
   *
   * @param message the message to include int eh response
   * @return the response
   */
  public static Map<String, Object> createOKResponseMap(String message) {
    Map<String, Object> response = new HashMap<String, Object>();
    response.put(RESPONSE_TYPE_KEY, WekaServlet.RESPONSE_OK);
    response.put(RESPONSE_MESSAGE_KEY, message);

    return response;
  }

  /**
   * Creates and adds a payload map to the supplied response map. Overwrites any existing
   * payload map in the supplied response
   *
   * @param responseMap the response map to add payload to
   * @param payloadTypeIdentifier the type of payload (added to the payload map
   *          under the {@code PAYLOAD_TYPE_KEY})
   * @return the payload map that was added
   */
  public static Map<String, Object> addPayloadMap(
    Map<String, Object> responseMap, String payloadTypeIdentifier) {
    Map<String, Object> payload = new HashMap<String, Object>();
    payload.put(PAYLOAD_TYPE_KEY, payloadTypeIdentifier);
    responseMap.put(RESPONSE_PAYLOAD_KEY, payload);

    return payload;
  }

  /**
   * Adds a supplied payload map to the supplied response map. Overwrites any
   * existing payload map.
   *
   * @param responseMap the response map to add the payload to
   * @param payloadMap the payload map to add
   * @param payloadIdentifier the type of payload (added to the payload map
   *          under the {@code PAYLOAD_TYPE_KEY})
   */
  public static void addPayloadMap(Map<String, Object> responseMap,
    Map<String, Object> payloadMap, String payloadIdentifier) {

    payloadMap.put(PAYLOAD_TYPE_KEY, payloadIdentifier);
    responseMap.put(RESPONSE_PAYLOAD_KEY, payloadMap);
  }

  /**
   * Encode a map to JSON
   *
   * @param toEncode the map to encode
   * @return the encoded map
   */
  public static String encodeToJSONString(Map<String, Object> toEncode) {
    ObjectMapper mapper = JsonFactory.create();
    return mapper.writeValueAsString(toEncode);
  }

  /**
   * Convert a JSON encoded {@code NamedTask} to a {@code NamedTask} object
   *
   * @param jsonTask the task in JSON format
   * @return a {@code NamedTask} instance
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  public static NamedTask jsonToNamedTask(String jsonTask) throws WekaException {
    if (jsonTask == null || jsonTask.length() == 0) {
      throw new WekaException("JSON task has size 0!");
    }

    ObjectMapper mapper = JsonFactory.create();
    Map<String, Object> taskMap = mapper.readValue(jsonTask, Map.class);

    String taskType = taskMap.get(TASK_TYPE_KEY).toString();
    NamedTask result = null;
    if (taskType.equals(TASK_TYPE_KF)) {
      result = jsonMapToKFTask(taskMap);
    } else {
      // TODO - other Task implementations supported by the JSON protocol and the
      // server
    }

    return result;
  }

  /**
   * Construct an unscheduled or scheduled Knowledge Flow task from the supplied
   * taskMap. <br>
   * <br>
   *
   * <pre>
   *         {
   *             taskName: name,
   *             taskType: type,
   *             schedule: [schedule json | null]
   *             kfFlow: kf json flow (as a sub map)
   *             sequential: [true | false]
   *             envVars: [environment variables json | null]
   *         }
   * </pre>
   *
   *
   *
   * @param taskMap map representation of the json source
   * @return a Knowledge Flow task
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected static NamedTask jsonMapToKFTask(Map<String, Object> taskMap)
    throws WekaException {
    NamedTask result = null;

    String name = taskMap.get(TASK_NAME_KEY).toString();
    // Object base64Flow = taskMap.get(KF_FLOW_PAYLOAD_ID);
    /*
     * if (base64Flow == null || base64Flow.toString().length() == 0) { throw
     * new WekaException("No flow resent in task!"); }
     */

    Map<String, String> envMap =
      (Map<String, String>) taskMap.get(ENV_PAYLOAD_ID);

    // have to transfer over to a new map because
    // org.boon.core.value.LazyValueMap
    // is not serializable
    Map<String, String> envMapNew = new HashMap<String, String>();
    envMapNew.putAll(envMap);

    Boolean sequential = (Boolean) taskMap.get(KF_SEQUENTIAL_PROPERTY);

    Object scheduleMap = taskMap.get(SCHEDULE_PAYLOAD_ID);

    // get flow as a string
    String jsonFlow = null;
    Map<String, Object> flowMap =
      (Map<String, Object>) taskMap.get(KF_FLOW_PAYLOAD_ID);
    if (flowMap == null) {
      throw new WekaException("No flow present in task!");
    }
    ObjectMapper mapper = JsonFactory.create();
    jsonFlow = mapper.writeValueAsString(flowMap);

    result =
      new UnscheduledNamedKnowledgeFlowTask(name, jsonFlow, sequential,
        envMapNew);
    if (scheduleMap != null) {
      Schedule schedule = jsonMapToSchedule((Map<String, Object>) scheduleMap);
      ScheduledNamedKnowledgeFlowTask newTask =
        new ScheduledNamedKnowledgeFlowTask(
          (UnscheduledNamedKnowledgeFlowTask) result, schedule);
      result = newTask;
    }

    return result;
  }

  /**
   * Convert a supplied Knowledge Flow named task (scheduled or unscheduled)
   * into a json map
   * 
   * @param task the task to convert
   * @return the map representation of the task
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> kFTaskToJsonMap(NamedTask task) {
    Schedule schedule =
      task instanceof ScheduledNamedKnowledgeFlowTask ? ((ScheduledNamedKnowledgeFlowTask) task)
        .getSchedule() : null;

    Map<String, Object> result = new HashMap<String, Object>();
    result.put(TASK_TYPE_KEY, TASK_TYPE_KF);
    String taskName = task.getName();
    result.put(TASK_NAME_KEY, taskName);

    Map<String, String> envMap =
      task instanceof ScheduledNamedKnowledgeFlowTask ? ((ScheduledNamedKnowledgeFlowTask) task)
        .getParameters() : ((UnscheduledNamedKnowledgeFlowTask) task)
        .getParameters();
    if (envMap != null) {
      result.put(ENV_PAYLOAD_ID, envMap);
    }
    boolean sequential =
      task instanceof ScheduledNamedKnowledgeFlowTask ? ((ScheduledNamedKnowledgeFlowTask) task)
        .getSequentialExecution() : ((UnscheduledNamedKnowledgeFlowTask) task)
        .getSequentialExecution();
    result.put(KF_SEQUENTIAL_PROPERTY, sequential);
    if (schedule != null) {
      Map<String, Object> scheduleMap = scheduleToJsonMap(schedule);
      result.put(SCHEDULE_PAYLOAD_ID, scheduleMap);
    }

    String jsonFlow =
      task instanceof UnscheduledNamedKnowledgeFlowTask ? ((UnscheduledNamedKnowledgeFlowTask) task)
        .getFlowJSON() : ((ScheduledNamedKnowledgeFlowTask) task).getFlowJSON();

    ObjectMapper mapper = JsonFactory.create();
    Map<String, Object> flowMap = mapper.readValue(jsonFlow, Map.class);
    result.put(KF_FLOW_PAYLOAD_ID, flowMap);

    return result;
  }

  /**
   * Construct a Schedule object from the supplied json map.
   *
   * @param scheduleMap map representation of a schedule
   * @return a {@code Schedule} object
   * @throws WekaException if a problem occurs
   */
  public static Schedule jsonMapToSchedule(Map<String, Object> scheduleMap)
    throws WekaException {
    Schedule schedule = new Schedule();
    if (scheduleMap.containsKey(SCHEDULE_DATE_FORMAT_KEY)) {
      schedule.m_dateFormat =
        scheduleMap.get(SCHEDULE_DATE_FORMAT_KEY).toString();
    }

    try {
      schedule.setStartDate(schedule.parseDate(scheduleMap
        .get(SCHEDULE_START_DATE_KEY)));

      schedule.setEndDate(schedule.parseDate(scheduleMap
        .get(SCHEDULE_END_DATE_KEY)));

      if (scheduleMap.containsKey(SCHEDULE_REPEAT_UNIT_KEY)) {
        Schedule.Repeat r =
          Schedule.Repeat.stringToValue(scheduleMap.get(
            SCHEDULE_REPEAT_UNIT_KEY).toString());
        if (r != null) {
          schedule.setRepeatUnit(r);
        }
      }

      if (scheduleMap.containsKey(SCHEDULE_REPEAT_VALUE_KEY)) {
        schedule.setRepeatValue((Integer) scheduleMap
          .get(SCHEDULE_REPEAT_VALUE_KEY));
      }

      if (scheduleMap.containsKey(SCHEDULE_DAY_OF_WEEK_KEY)) {
        String csvDays = scheduleMap.get(SCHEDULE_DAY_OF_WEEK_KEY).toString();
        String[] days = csvDays.split(",");
        List<Integer> dowl = new ArrayList<Integer>();
        for (String dow : days) {
          dowl.add(Schedule.stringDOWToCalendarDOW(dow));
        }
        schedule.setDayOfTheWeek(dowl);
      }

      if (scheduleMap.containsKey(SCHEDULE_MONTH_OF_YEAR_KEY)) {
        schedule.setMonthOfTheYear(Schedule
          .stringMonthToCalendarMonth(scheduleMap.get(
            SCHEDULE_MONTH_OF_YEAR_KEY).toString()));
      }

      if (scheduleMap.containsKey(SCHEDULE_DAY_OF_MONTH_KEY)) {
        schedule.setDayOfTheMonth(Integer.parseInt(scheduleMap.get(
          SCHEDULE_DAY_OF_MONTH_KEY).toString()));
      }

      if (scheduleMap.containsKey(SCHEDULE_OCCURRENCE_KEY)) {
        Schedule.OccurrenceWithinMonth occ =
          Schedule.OccurrenceWithinMonth.stringToValue(scheduleMap.get(
            SCHEDULE_OCCURRENCE_KEY).toString());
        if (occ != null) {
          schedule.setOccurrenceWithinMonth(occ);
        }
      }
    } catch (ParseException e) {
      throw new WekaException(e);
    }

    return schedule;
  }

  /**
   * Convert a schedule object to a map (ready for encoding to a json string)
   *
   * @param schedule the schedule to convert
   * @return a map
   */
  protected static Map<String, Object> scheduleToJsonMap(Schedule schedule) {
    Map<String, Object> scheduleMap = new HashMap<String, Object>();

    scheduleMap.put(SCHEDULE_DATE_FORMAT_KEY, schedule.m_dateFormat);
    SimpleDateFormat sdf = new SimpleDateFormat(schedule.m_dateFormat);
    if (schedule.getStartDate() != null) {
      scheduleMap.put(SCHEDULE_START_DATE_KEY,
        sdf.format(schedule.getStartDate()));
    }
    if (schedule.getEndDate() != null) {
      scheduleMap.put(SCHEDULE_END_DATE_KEY, sdf.format(schedule.getEndDate()));
    }

    scheduleMap.put(SCHEDULE_REPEAT_UNIT_KEY, schedule.getRepeatUnit());
    scheduleMap.put(SCHEDULE_REPEAT_VALUE_KEY, schedule.getRepeatValue());
    if (schedule.getDayOfTheWeek() != null
      && schedule.getDayOfTheWeek().size() > 0) {
      String dowList = "";
      for (int dow : schedule.getDayOfTheWeek()) {
        dowList += "," + Schedule.calendarDOWToString(dow);
      }
      dowList = dowList.substring(1);
      scheduleMap.put(SCHEDULE_DAY_OF_WEEK_KEY, dowList);
    }

    if (schedule.getMonthOfTheYear() >= Calendar.JANUARY
      && schedule.getMonthOfTheYear() <= Calendar.DECEMBER) {
      scheduleMap.put(SCHEDULE_MONTH_OF_YEAR_KEY,
        Schedule.calendarMonthToString(schedule.getMonthOfTheYear()));
    }

    scheduleMap.put(SCHEDULE_DAY_OF_MONTH_KEY, schedule.getDayOfTheMonth());
    if (schedule.getOccurrenceWithinMonth() != null) {
      scheduleMap.put(SCHEDULE_OCCURRENCE_KEY, schedule
        .getOccurrenceWithinMonth().toString());
    }

    return scheduleMap;
  }

  /**
   * Convert a {@code TaskStatusInfo} object to a map (ready for encoding to a
   * json string)
   *
   * @param statusInfo the status info object to convert
   * @param serializeResult true if the result object from the task status info
   *          should be serialized to base64
   * @return a map containing the parts of the task status info
   * @throws Exception if a problem occurs
   */
  protected static Map<String, Object> taskStatusInfoToJsonMap(
    TaskStatusInfo statusInfo, boolean serializeResult) throws Exception {
    Map<String, Object> statusMap = new HashMap<String, Object>();

    statusMap.put(TASK_STATUS_INFO_EXECUTION_STATUS_KEY,
      statusInfo.getExecutionStatus());
    statusMap.put(TASK_STATUS_INFO_STATUS_MESSAGE_KEY,
      statusInfo.getStatusMessage());
    if (serializeResult) {
      String base64Compressed = encodeToBase64(statusInfo.getTaskResult());
      statusMap.put(TASK_STATUS_INFO_TASK_RESULT_KEY, base64Compressed);
    }

    return statusMap;
  }

  /**
   * Construct a {@code TaskStatusInfo} object from the supplied map
   *
   * @param statusInfo map representation of a {@code TaskStatusInfo}
   * @return the reconstructed {@code TaskStatusInfo} object
   */
  public static TaskStatusInfo jsonMapToStatusInfo(
    Map<String, Object> statusInfo) {
    TaskStatusInfo result = new TaskStatusInfo();
    result.setExecutionStatus((Integer) statusInfo
      .get(TASK_STATUS_INFO_EXECUTION_STATUS_KEY));
    result.setStatusMessage(statusInfo.get(TASK_STATUS_INFO_STATUS_MESSAGE_KEY)
      .toString());
    Object base64Compressed = statusInfo.get(TASK_STATUS_INFO_TASK_RESULT_KEY);
    if (base64Compressed != null && base64Compressed.toString().length() > 0) {
      result.setTaskResult(base64Compressed.toString());
    }

    return result;
  }

  /**
   * Encode an object to a base64 string of compressed bytes
   *
   * @param toEncode the object to encode
   * @return the encoded string
   * @throws IOException if a problem occurs
   */
  protected static String encodeToBase64(Object toEncode) throws IOException {
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    BufferedOutputStream bos = new BufferedOutputStream(bao);
    ObjectOutputStream oo = new ObjectOutputStream(bos);
    oo.writeObject(toEncode);
    oo.flush();
    byte[] val = bao.toByteArray();

    String string;
    if (val == null) {
      string = null;
    } else {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      GZIPOutputStream gzos = new GZIPOutputStream(baos);
      bos = new BufferedOutputStream(gzos);
      bos.write(val);
      bos.flush();
      bos.close();

      string = new String(Base64.encodeBase64(baos.toByteArray()));
    }
    return string;
  }

  /**
   * Decode a base64 string (containing compressed bytes) back into an object
   *
   * @param base64 the base64 string
   * @return an decoded and deserialized object
   * @throws Exception if a problem occurs
   */
  public static Object decodeBase64(String base64) throws Exception {
    byte[] bytes = Base64.decodeBase64(base64.getBytes());

    byte[] result = null;
    Object resultObject = null;
    if (bytes != null && bytes.length > 0) {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      GZIPInputStream gzip = new GZIPInputStream(bais);
      BufferedInputStream bi = new BufferedInputStream(gzip);

      result = new byte[] {};

      byte[] extra = new byte[1000000];
      int nrExtra = bi.read(extra);

      while (nrExtra >= 0) {
        // add it to bytes...
        int newSize = result.length + nrExtra;
        byte[] tmp = new byte[newSize];
        for (int i = 0; i < result.length; i++) {
          tmp[i] = result[i];
        }
        for (int i = 0; i < nrExtra; i++) {
          tmp[result.length + i] = extra[i];
        }

        // change the result
        result = tmp;
        nrExtra = bi.read(extra);
      }
      bytes = result;
      gzip.close();
    }

    if (result != null && result.length > 0) {
      ByteArrayInputStream bis = new ByteArrayInputStream(result);
      ObjectInputStream ois = new ObjectInputStream(bis);
      resultObject = ois.readObject();
      ois.close();
    }

    return resultObject;
  }
}
