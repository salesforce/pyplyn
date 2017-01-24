/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.appconfig;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Reads config file name from CLI args
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
final public class AppConfigFileLoader {

    /**
     * Helper classes should not be instantiated
     */
    private AppConfigFileLoader() { }

    /**
     * Reads a configuration file from the passed CLI args
     *
     * @return Path to config file, or exception
     * @throws ConfigParseException if arguments were incorrectly specified, or nothing was passed (in which case it will print "usage" details)
     */
    public static String loadFromCLI(String programName, String... args) throws ConfigParseException {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("java -jar " + programName + "-[VERSION].jar").defaultHelp(true);
        parser.addArgument("--config").metavar("/path/to/app-config.json").required(true).help("Path to configuration file");

        try {
            // parse CLI args and return config file
            Namespace cli = parser.parseArgs(args);
            return cli.getString("config");

        } catch (ArgumentParserException e) {
            // show help message and stop execution
            parser.handleError(e);
            throw new ConfigParseException(e);
        }
    }
}
