package org.rabix.tests;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.rabix.common.helper.JSONHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRunner {
	private static String testDirPath;
	private static String cmdPrefix;
	private static String buildFile;
	private static String currentTestSuite;
	private static String integrationTempResultPath = "./rabix-backend-local/target/result.yaml";
	private static String workingdir = "./rabix-backend-local/target/";
	private static String cwlTestWorkingdir;
	private static String draftName;
	private static final Logger logger = LoggerFactory.getLogger(TestRunner.class);

	public static void main(String[] commandLineArguments) {
		try {
			draftName = commandLineArguments[0];
			startIntegrationTests(draftName);
			
			if (!draftName.equals("draft-sb")) {
				startConformanceTests(draftName);
			}

		} catch (RabixTestException e) {
			logger.error("Error occuerred:", e);
			System.exit(-1);
		}
	}

	private static void startConformanceTests(String draftName) throws RabixTestException {
		logger.info("Conformance tests started:  " + draftName);
		PropertiesConfiguration configuration = getConfig();
		cwlTestWorkingdir = getStringFromConfig(configuration, draftName);
		
		String starterScriptName = draftName +"_starter.sh";
		String commandCopyCwlStarter = "cp " + System.getProperty("user.dir") + "/rabix-integration-testing/cwlstarter/" + starterScriptName + " .";
		
		command(commandCopyCwlStarter, cwlTestWorkingdir);
		
		logger.info(" --- Running conformance tests: " + draftName);
		logger.info(" --- Conformance working dir: " + cwlTestWorkingdir);
		logger.info(" --- Conformance starter script: " + starterScriptName);
		
		command("chmod +x " + starterScriptName , cwlTestWorkingdir);
		//TODO DEBUG
		command("echo checkit ./rabix-integration-testing/ ", ".");
		command("ls -ltra", "./rabix-integration-testing/");
		
		command("echo checkit ./rabix-integration-testing/common-workflow-language/draft-2/ ", ".");
		command("ls -ltra", "./rabix-integration-testing/common-workflow-language/draft-2/");
		
		
		//TODO END DEBUG
		executeConformanceSuite("export PATH=$PATH:`pwd`", "/home/travis/build/markosbg/debug/rabix-backend-local/target/");
		executeConformanceSuite("./" + starterScriptName, cwlTestWorkingdir);
		logger.info("Conformance test ended: " + draftName);
		
	}

	private static void startIntegrationTests(String draftName) throws RabixTestException {
		logger.info("Integration tests started:  " + draftName);
		boolean allTestsPassed = true;
		boolean testPassed = false;
		PropertiesConfiguration configuration = getConfig();
		setupIntegrationTestDirPath(configuration, draftName);
		setupIntegrationCommandPrefix(configuration);
		setupBuildFilePath(configuration);
		
		File dir = new File(testDirPath);
		if (!dir.isDirectory()) {
			logger.error("Problem with test directory path: Test directory path is not valid directory path.");
			System.exit(-1);
		}
		File[] directoryListing = dir.listFiles();
		if (directoryListing == null) {
			logger.error("Problem with provided test directory: Test directory is empty.");
		}
		
		ArrayList<Object> failedTests = new ArrayList<Object>();
		
		extractBuildFile();
		copyTestbacklog();

		for (File child : directoryListing) {
			if (!child.getPath().endsWith(".test.yaml"))
				continue;
			try {

				currentTestSuite = child.getPath();
				logger.info(" ### ");
				logger.info("Executing test suite: " + currentTestSuite);
				String currentTest = readFile(child.getAbsolutePath(), Charset.defaultCharset());
				Map<String, Object> inputSuite = JSONHelper.readMap(JSONHelper.transformToJSON(currentTest));
				Iterator entries = inputSuite.entrySet().iterator();

				while (entries.hasNext()) {
					Entry thisEntry = (Entry) entries.next();
					Object testName = thisEntry.getKey();
					Object test = thisEntry.getValue();
					logger.info(" --- ");
					logger.info("Running test: " + testName + " with given parameters:");
					@SuppressWarnings({ "rawtypes", "unchecked" })
					Map<String, Map<String, LinkedHashMap>> mapTest = (Map<String, Map<String, LinkedHashMap>>) test;
					logger.info("  app: " + mapTest.get("app"));
					logger.info("  inputs: " + mapTest.get("inputs"));
					logger.info("  expected: " + mapTest.get("expected"));
					String cmd = cmdPrefix + " " + mapTest.get("app") + " " + mapTest.get("inputs") + " -v > result.yaml";
					logger.info("->Running cmd: " + cmd);
					command(cmd, workingdir);

					File integrationTempResultFile = new File(integrationTempResultPath);

					String resultText = readFile(integrationTempResultFile.getAbsolutePath(), Charset.defaultCharset());
					Map<String, Object> resultData = JSONHelper.readMap(JSONHelper.transformToJSON(resultText));
					logger.info("\nGenerated result file:");
					logger.info(resultText);
					testPassed = validateTestCase(mapTest, resultData);
					logger.info("Test result: ");
					if (testPassed) {
						logger.info(testName + " PASSED");

					} else {
						logger.info(testName + " FAILED");
						failedTests.add(testName);
						allTestsPassed = false;
					}

				}

				if (allTestsPassed) {
					logger.info("");
					logger.info("Test suite: " + currentTestSuite + ", passed successfully.");
				} else {
					logger.info("");
					logger.info("Test suite " + currentTestSuite + ", failed:");
					logger.info("Failed test number: " + failedTests.size());
					logger.info("Failed tests:");
					for (Object test : failedTests) {
						logger.info(test.toString());
					}

				}

			} catch (IOException e) {
				logger.error("Test suite: " + currentTestSuite + ", execution failed. ", e);
				System.exit(-1);
			}
		}
		logger.info("Integration tests finished:  " + draftName);
	}

	private static void copyTestbacklog() throws RabixTestException {
		String commandCopyTestbacklog = "cp -a " + System.getProperty("user.dir")
				+ "/rabix-integration-testing/testbacklog .";
		command(commandCopyTestbacklog, workingdir);
		logger.info("Copying testbacklog command: " + commandCopyTestbacklog);
		logger.info("Copying testbacklog dir: done ");
	}

	private static void extractBuildFile() throws RabixTestException {
		logger.info("Extracting build file: started");

		String commandUntarBuildFile = "tar -zxvf " + System.getProperty("user.dir") + buildFile;

		File extractDir = new File(System.getProperty("user.dir") + "/rabix-backend-local/target/");
		File fileToExtract = new File(System.getProperty("user.dir") + buildFile);

		logger.info("Checking extract dir path: " + extractDir.getAbsolutePath());
		if (!extractDir.isDirectory()) {
			logger.error(
					"Problem with extract directory path: Test extract directory path is not valid directory path.");
			System.exit(-1);
		}
		logger.info("Checking extract file path: " + fileToExtract.getAbsolutePath());
		if (!fileToExtract.isFile()) {
			logger.error("Problem with extract file path: Test extract file path is not valid directory path.");
			System.exit(-1);
		}

		logger.info("Extracting build file command: " + commandUntarBuildFile);
		command(commandUntarBuildFile, System.getProperty("user.dir") + "/rabix-backend-local/target/");
		logger.info("Extracting build file: ended");
	}

	private static void setupBuildFilePath(PropertiesConfiguration configuration) {
		buildFile = getStringFromConfig(configuration, "buildFile");
	}

	private static void setupIntegrationCommandPrefix(PropertiesConfiguration configuration) {
		cmdPrefix = getStringFromConfig(configuration, "cmdPrefix");
	}

	private static void setupIntegrationTestDirPath(PropertiesConfiguration configuration, String draftName) {
		testDirPath = getStringFromConfig(configuration, "testDirPath_" + draftName);
	}

	private static boolean validateTestCase(Map<String, Map<String, LinkedHashMap>> mapTest,
			Map<String, Object> resultData) {
		String resultFileName;
		int resultFileSize;
		String resultFileClass;
		Map<String, Object> resultValues = ((Map<String, Object>) resultData.get("output"));
		resultFileName = resultValues.get("path").toString();
		resultFileName = resultFileName.split("/")[resultFileName.split("/").length - 1];
		resultFileSize = (int) resultValues.get("size");
		resultFileClass = resultValues.get("class").toString();
		logger.info("Test validation:");
		logger.info("result file name: " + resultFileName + ", expected file name: "
				+ mapTest.get("expected").get("outfile").get("name"));
		logger.info("result file size: " + resultFileSize + ", expected file size: "
				+ mapTest.get("expected").get("outfile").get("size"));
		logger.info("result file class: " + resultFileClass + ", expected file class: "
				+ mapTest.get("expected").get("outfile").get("class"));

		boolean fileNamesEqual = resultFileName.equals(mapTest.get("expected").get("outfile").get("name"));
		boolean fileSizesEqual = resultFileSize == (int) mapTest.get("expected").get("outfile").get("size");
		boolean fileClassesEqual = resultFileClass.equals(mapTest.get("expected").get("outfile").get("class"));

		if (!fileNamesEqual) {
			logger.error("result and expected file name are not equal!");
		} else {
			if (!fileSizesEqual) {
				logger.error("result and expected file size are not equal!");
			} else {
				if (!fileClassesEqual) {
					logger.error("result and expected file class are not equal!");
				} else {
					logger.info("Test case passed.");
					return true;
				}
			}
		}

		return false;
	}

	public static void command(final String cmdline, final String directory) throws RabixTestException {
		try {
			Process process = new ProcessBuilder(new String[] { "bash", "-c", cmdline }).inheritIO()
					.directory(new File(directory)).start();

			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null)
				logger.info(line);

			int exitCode = process.waitFor();

			if (0 != exitCode) {
				File resultFile = new File(integrationTempResultPath);
				String stdErr = readFile(resultFile.getAbsolutePath(), Charset.defaultCharset());
				logger.info("ovde!!");
				logger.error(stdErr);
				throw new RabixTestException("Error while executing command: Non zero exit code " + exitCode);
			}

		} catch (Exception e) {
			logger.error("Error while executing command. ", e);
			throw new RabixTestException("Error while executing command: " + e.getMessage());
		}
	}
	
	public static void executeConformanceSuite(final String cmdline, final String directory) throws RabixTestException {
		try {
			Process process = new ProcessBuilder(new String[] { "bash", "-c", cmdline }).inheritIO()
					.directory(new File(directory)).start();

			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null)
				logger.info(line);

			int exitCode = process.waitFor();

			if (0 != exitCode) {
//				File resultConfFile = new File("/home/travis/build/markosbg/debug/rabix-integration-testing/common-workflow-language/draft-2/resultConf.txt");
//				String stdErr = readFile(resultConfFile.getAbsolutePath(), Charset.defaultCharset());
//				logger.info("ovdeconf!");
//				logger.error(stdErr);
				throw new RabixTestException("Error while executing command: Non zero exit code " + exitCode);
			}

		} catch (Exception e) {
			logger.error("Error while executing command. ", e);
			throw new RabixTestException("Error while executing command: " + e.getMessage());
		}
	}

	/**
	 * Reads content from a file
	 */
	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	@SuppressWarnings("unchecked")
	private static PropertiesConfiguration getConfig() throws RabixTestException {
		PropertiesConfiguration configuration = new PropertiesConfiguration();
		String userDir = System.getProperty("user.dir");
		if (userDir == null) {
			throw new RabixTestException("null value for user.dir property");
		}
		File configDir = new File(userDir + "/rabix-integration-testing/config/test");
		try {
			Iterator<File> iterator = FileUtils.iterateFiles(configDir, new String[] { "properties" }, true);
			while (iterator.hasNext()) {
				File configFile = iterator.next();
				configuration.load(configFile);
			}
			return configuration;
		} catch (ConfigurationException e) {
			logger.error("Failed to load configuration properties", e);
			throw new RabixTestException("Failed to load configuration properties");
		}
	}

	private static String getStringFromConfig(PropertiesConfiguration configuration, String key) {
		return configuration.getString(key);
	}

}
