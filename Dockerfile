# Use an official OpenJDK runtime as a parent image
# Pin to jammy so apt tooling stays compatible (floating :11 now tracks Ubuntu Resolute)
FROM eclipse-temurin:11-jdk-jammy

# Set the working directory in the container
WORKDIR /app

# Copy the current directory contents into the container at /app
COPY . /app

# Install sbt (apt-key is removed on modern Ubuntu; use signed-by keyring instead)
RUN apt-get update && \
    apt-get install -y curl gnupg && \
    install -d -m 0755 /etc/apt/keyrings && \
    curl -fsSL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --dearmor -o /etc/apt/keyrings/scalasbt.gpg && \
    echo "deb [signed-by=/etc/apt/keyrings/scalasbt.gpg] https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    apt-get update && \
    apt-get install -y sbt && \
    rm -rf /var/lib/apt/lists/*

# Expose the port the app runs on
EXPOSE 9000

# Build the project
RUN sbt stage

# Run the Play app
CMD ["sbt", "run"]
