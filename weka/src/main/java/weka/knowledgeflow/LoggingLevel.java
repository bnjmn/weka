package weka.knowledgeflow;

public enum LoggingLevel {
  NONE("None"), LOW("Low"), BASIC("Basic"), DETAILED("Detailed"), DEBUGGING(
    "Debugging"), WARNING("WARNING"), ERROR("ERROR");

  private final String m_name;

  LoggingLevel(String name) {
    m_name = name;
  }

  public static LoggingLevel stringToLevel(String s) {
    LoggingLevel ret = LoggingLevel.BASIC;
    for (LoggingLevel l : LoggingLevel.values()) {
      if (l.toString().equals(s)) {
        ret = l;
      }
    }
    return ret;
  }

  @Override
  public String toString() {
    return m_name;
  }
}
