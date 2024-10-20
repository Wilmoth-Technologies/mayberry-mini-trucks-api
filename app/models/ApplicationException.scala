package models

case class ApplicationException(inputMessage: String, exceptionMessage: String, exception: Throwable, source: String = "") extends RuntimeException