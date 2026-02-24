plugins {
	java
	id("org.springframework.boot") version "4.0.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "Demo TaskManagerApplication for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation") // @Valid, @NotBlank, @Size
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	runtimeOnly("org.postgresql:postgresql")

	// ... other dependencies
	implementation("org.mapstruct:mapstruct:1.6.0.Beta1") // Use latest for Java 25
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.0.Beta1")

	// Crucial: MapStruct must work with Lombok
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
	implementation("org.owasp.antisamy:antisamy:1.7.4")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
