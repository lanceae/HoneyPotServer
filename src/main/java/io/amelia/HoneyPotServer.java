/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2018 Amelia Sara Greene <barelyaprincess@gmail.com>
 * Copyright (c) 2018 Penoaks Publishing LLC <development@penoaks.com>
 * <p>
 * All Rights Reserved.
 */
package io.amelia;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;

import io.amelia.bindings.Bindings;
import io.amelia.data.parcel.ParcelCarrier;
import io.amelia.foundation.ConfigRegistry;
import io.amelia.foundation.DefaultApplication;
import io.amelia.foundation.Env;
import io.amelia.foundation.Foundation;
import io.amelia.foundation.Kernel;
import io.amelia.foundation.NetworkedApplication;
import io.amelia.foundation.PropDevMeta;
import io.amelia.foundation.Runlevel;
import io.amelia.lang.ApplicationException;
import io.amelia.lang.ParcelException;
import io.amelia.lang.StorageException;
import io.amelia.logcompat.DefaultLogFormatter;
import io.amelia.logcompat.LogBuilder;
import io.amelia.looper.LooperRouter;
import io.amelia.net.NetworkLoader;
import io.amelia.plugins.BasePlugins;
import io.amelia.storage.HoneyStorage;
import io.amelia.storage.HoneyStorageProvider;
import io.amelia.storage.backend.FileStorageBackend;
import io.amelia.storage.backend.StorageBackend;
import io.amelia.support.EnumColor;
import io.amelia.support.IO;
import io.amelia.support.NodePath;
import io.amelia.support.Streams;
import io.amelia.support.Sys;
import io.amelia.users.Users;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import static io.amelia.foundation.Runlevel.INITIALIZATION;
import static io.amelia.foundation.Runlevel.MAINLOOP;
import static io.amelia.foundation.Runlevel.NETWORKING;
import static io.amelia.foundation.Runlevel.SHUTDOWN;

public class HoneyPotServer extends DefaultApplication implements NetworkedApplication
{
	/**
	 * Provides shortcut CONST for keyed directory paths
	 */
	public static final String PATH_WEBROOT = "__webroot";
	public static final String PATH_DATABASE = "__database";
	public static final String PATH_SESSIONS = "__sessions";

	public HoneyPotServer() throws ApplicationException.Error
	{
		/* Register keyed directory paths with the ConfigRegistry */
		Kernel.setPath( PATH_WEBROOT, Kernel.PATH_APP, "webroot" );
		Kernel.setPath( PATH_DATABASE, Kernel.PATH_APP, "database" );
		Kernel.setPath( PATH_SESSIONS, Kernel.PATH_STORAGE, "sessions" );

		Kernel.setDevMeta( new PropDevMeta( HoneyPotServer.class, "build.properties" ) );

		addArgument( "console-fancy", "Specifies if control characters are written with console output to stylize it, e.g., fgcolor, bgcolor, bold, or inverted." );
		addStringArgument( "cluster-id", "Specifies the cluster unique identity" );
		addStringArgument( "instance-id", "Specifies the instance unique identity" );
		addIntegerArgument( "http-port", "Override Unsecure HTTP port number found in data" );
		addIntegerArgument( "https-port", "Override Secure HTTPS port number found in data" );
	}

	@Override
	public void handleParcel( ParcelCarrier parcelCarrier ) throws ParcelException.Error
	{
		// TODO Nothing Yet!
	}

	@Override
	public void onRunlevelChange( Runlevel previousRunlevel, Runlevel currentRunlevel ) throws ApplicationException.Error
	{
		super.onRunlevelChange( previousRunlevel, currentRunlevel );

		if ( currentRunlevel == INITIALIZATION )
		{
			if ( Sys.isAdminUser() )
				Kernel.L.warning( "We detected that you are running this application with the system admin account. This is highly discouraged, it may compromise security or file permissions." );

			// Minimum 1GB memory recommended.
			if ( Runtime.getRuntime().maxMemory() / 1024L / 1024L < 1024L )
				LogBuilder.get().warning( "We detected less than the recommended 1024Mb of JVM ram, we recommended you dedicate more ram to guarantee a smoother experience. You can use the JVM options \"-Xmx1024M -Xms1024M\" to set the ram at 1GB." );

			try
			{
				Env env = getEnv();

				LogBuilder.setConsoleFormatter( new DefaultLogFormatter( env.getBoolean( "console-fancy" ).orElse( true ) ) );
			}
			catch ( Exception e )
			{
				throwStartupException( e );
			}

			// TODO Install or factory reset option?

			// TODO Extract Web UI archive to webroot.

			if ( Sys.isUnixLikeOS() )
			{
				SignalHandler signalHandler = signal -> Foundation.shutdown( "Received SIGTERM - Terminate" );

				Signal.handle( new Signal( "TERM" ), signalHandler );
				Signal.handle( new Signal( "INT" ), signalHandler );
			}
		}
		if ( currentRunlevel == MAINLOOP )
			LooperRouter.getMainLooper().postTaskRepeatingLater( NetworkLoader::heartbeat, 50L, 50L );
		if ( currentRunlevel == SHUTDOWN )
		{

			// LogBuilder.get().info( "Shutting Down Plugin Manager..." );
			// PluginManager.shutdown();

			// LogBuilder.get().info( "Shutting Down Permission Manager..." );
			// PermissionDispatcher.shutdown();

			// LogBuilder.get().info( "Shutting Down Account Manager..." );
			// AccountManager.shutdown();
		}
		if ( currentRunlevel == NETWORKING )
		{
			NetworkLoader.init();
		}
	}

	@Override
	protected void parse() throws Exception
	{
		Streams.forEachWithException( ConfigRegistry.config.getChild( Users.ConfigKeys.CREATORS ).getChildren(), child -> {
			URI userCreatorPath = URI.create( ConfigRegistry.config.getString( "path" ).orElseThrow( () -> new StorageException.Error( "Malformed user creator configuration. {backend=" + child.getCurrentPath() + "}" ) ) );
			StorageBackend storageBackend;

			if ( HoneyStorageProvider.SCHEME.equals( userCreatorPath.getScheme() ) )
				storageBackend = HoneyStorage.getBackend( NodePath.of( userCreatorPath.getPath() ) ).orElseThrow( () -> new StorageException.Error( "The user creator " + userCreatorPath + " was not found." ) );
			else
				storageBackend = new FileStorageBackend( Paths.get( userCreatorPath ) );

			Foundation.getUsers().addUserCreator( child.getName(), storageBackend, child.getBoolean( "default" ).orElse( false ) );
		} );
	}

	public BasePlugins plugins()
	{
		return Bindings.resolveClass( BasePlugins.class ).orElseHandleCause( e -> Kernel.getExceptionRegistrar().fatalError( e.getExceptionReport(), true ), () -> null );
	}

	@Override
	public void sendToAll( ParcelCarrier parcel )
	{
		// TODO Distribute the parcel to all available receivers.
	}

	@Override
	public void showBanner( Kernel.Logger logger )
	{
		InputStream is = getClass().getClassLoader().getResourceAsStream( "banner.txt" );
		if ( is != null )
			for ( String line : IO.readStreamToLines( is ) )
				LogBuilder.get().info( EnumColor.GOLD + line );

		super.showBanner( logger );
	}

	public static class ConfigKeys
	{
		/**
		 * Specifies a config key for disabling a application metrics.
		 *
		 * <pre>
		 * foundation:
		 *   disableMetrics: false
		 * </pre>
		 */
		public static final String DISABLE_METRICS = "foundation.disableMetrics";

		private ConfigKeys()
		{
			// Static Access
		}
	}
}
