/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kurtraschke.ctatt.gtfsrealtime;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Parser;
import org.nnsoft.guice.rocoto.Rocoto;
import org.nnsoft.guice.rocoto.configuration.ConfigurationModule;
import org.nnsoft.guice.rocoto.converters.FileConverter;
import org.nnsoft.guice.rocoto.converters.PropertiesConverter;
import org.nnsoft.guice.rocoto.converters.URLConverter;
import org.slf4j.LoggerFactory;

import com.google.inject.ConfigurationException;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

import org.onebusaway.cli.CommandLineInterfaceLibrary;
import org.onebusaway.cli.Daemonizer;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeExporter;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeFileWriter;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeGuiceBindingTypes;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeServlet;
import org.onebusaway.guice.jsr250.LifecycleService;

/**
 *
 * @author kurt
 */
public class TrainTrackerRealtimeMain {

  private static final org.slf4j.Logger _log = LoggerFactory.getLogger(TrainTrackerRealtimeMain.class);
  private final String ARG_CONFIG_FILE = "config";
  private File _tripUpdatesPath;
  private URL _tripUpdatesUrl;
  private File _vehiclePositionsPath;
  private URL _vehiclePositionsUrl;
  private File _alertsPath;
  private URL _alertsUrl;
  private Injector _injector;
  private TrainTrackerRealtimeProvider _provider;
  private LifecycleService _lifecycleService;
  private GtfsRealtimeExporter _vehiclePositionsExporter;
  private GtfsRealtimeExporter _tripUpdatesExporter;
  private GtfsRealtimeExporter _alertsExporter;

  public static void main(String[] args) throws Exception {
    TrainTrackerRealtimeMain m = new TrainTrackerRealtimeMain();
    try {
      m.run(args);
    } catch (CreationException e) {
      e.printStackTrace(System.err);
      System.exit(-1);
    }
  }

  @Inject
  public void setProvider(TrainTrackerRealtimeProvider provider) {
    _provider = provider;
  }

  @Inject
  public void setLifecycleService(LifecycleService lifecycleService) {
    _lifecycleService = lifecycleService;
  }

  @Inject
  public void setVehiclePositionsExporter(@GtfsRealtimeGuiceBindingTypes.VehiclePositions GtfsRealtimeExporter exporter) {
    _vehiclePositionsExporter = exporter;
  }

  @Inject
  public void setTripUpdatesExporter(@GtfsRealtimeGuiceBindingTypes.TripUpdates GtfsRealtimeExporter exporter) {
    _tripUpdatesExporter = exporter;
  }

  @Inject
  public void setAlertsExporter(@GtfsRealtimeGuiceBindingTypes.Alerts GtfsRealtimeExporter exporter) {
    _alertsExporter = exporter;
  }

  public void run(String[] args) throws Exception {
    if (args.length == 0 || CommandLineInterfaceLibrary.wantsHelp(args)) {
      printUsage();
      System.exit(-1);
    }

    Options options = new Options();
    buildOptions(options);
    Daemonizer.buildOptions(options);
    Parser parser = new GnuParser();
    final CommandLine cli = parser.parse(options, args);
    Daemonizer.handleDaemonization(cli);

    Set<Module> modules = new HashSet<>();
    TrainTrackerRealtimeModule.addModuleAndDependencies(modules);

    _injector = Guice.createInjector(
            new URLConverter(),
            new FileConverter(),
            new PropertiesConverter(),
            new ConfigurationModule() {
      @Override
      protected void bindConfigurations() {
        bindEnvironmentVariables();
        bindSystemProperties();

        if (cli.hasOption(ARG_CONFIG_FILE)) {
          bindProperties(new File(cli.getOptionValue(ARG_CONFIG_FILE)));
        }
      }
    },
            Rocoto.expandVariables(modules));

    _injector.injectMembers(this);

    _tripUpdatesUrl = getConfigurationValue(URL.class, "tripUpdates.url");
    if (_tripUpdatesUrl != null) {
      GtfsRealtimeServlet servlet = _injector.getInstance(GtfsRealtimeServlet.class);
      servlet.setUrl(_tripUpdatesUrl);
      servlet.setSource(_tripUpdatesExporter);

    }

    _tripUpdatesPath = getConfigurationValue(File.class, "tripUpdates.path");
    if (_tripUpdatesPath != null) {
      GtfsRealtimeFileWriter writer = _injector.getInstance(GtfsRealtimeFileWriter.class);
      writer.setPath(_tripUpdatesPath);
      writer.setSource(_tripUpdatesExporter);
    }

    _vehiclePositionsUrl = getConfigurationValue(URL.class, "vehiclePositions.url");
    if (_vehiclePositionsUrl != null) {
      GtfsRealtimeServlet servlet = _injector.getInstance(GtfsRealtimeServlet.class);
      servlet.setUrl(_vehiclePositionsUrl);
      servlet.setSource(_vehiclePositionsExporter);
    }

    _vehiclePositionsPath = getConfigurationValue(File.class, "vehiclePositions.path");
    if (_vehiclePositionsPath != null) {
      GtfsRealtimeFileWriter writer = _injector.getInstance(GtfsRealtimeFileWriter.class);
      writer.setPath(_vehiclePositionsPath);
      writer.setSource(_vehiclePositionsExporter);
    }

    _alertsUrl = getConfigurationValue(URL.class, "alerts.url");
    if (_alertsUrl != null) {
      GtfsRealtimeServlet servlet = _injector.getInstance(GtfsRealtimeServlet.class);
      servlet.setUrl(_alertsUrl);
      servlet.setSource(_alertsExporter);
    }

    _alertsPath = getConfigurationValue(File.class, "alerts.path");
    if (_alertsPath != null) {
      GtfsRealtimeFileWriter writer = _injector.getInstance(GtfsRealtimeFileWriter.class);
      writer.setPath(_alertsPath);
      writer.setSource(_alertsExporter);
    }

    _lifecycleService.start();
  }

  private <T> T getConfigurationValue(Class<T> type, String configurationKey) {
    try {
      return _injector.getInstance(Key.get(type, Names.named(configurationKey)));
    } catch (ConfigurationException e) {
      return null;
    }
  }

  private void printUsage() {
    CommandLineInterfaceLibrary.printUsage(getClass());
  }

  private void buildOptions(Options options) {
    Option configFile = new Option(ARG_CONFIG_FILE, true, "configuration file path");
    configFile.setRequired(true);
    options.addOption(configFile);
  }
}