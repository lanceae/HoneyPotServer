/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * <p>
 * Copyright (c) 2019 Amelia Sara Greene <barelyaprincess@gmail.com>
 * Copyright (c) 2019 Penoaks Publishing LLC <development@penoaks.com>
 * <p>
 * All Rights Reserved.
 */
package io.amelia.monitor;

import io.amelia.events.Events;
import io.amelia.events.RunlevelEvent;
import io.amelia.foundation.Foundation;
import io.amelia.foundation.MinimalApplication;
import io.amelia.foundation.Runlevel;
import io.amelia.lang.ApplicationException;
import io.amelia.lang.StartupException;
import io.amelia.lang.StartupInterruptException;
import io.amelia.net.wip.NetworkLoader;
import io.amelia.net.wip.ipc.IPC;
import io.amelia.net.wip.packets.PacketValidationException;
import io.amelia.net.wip.udp.UDPWorker;

public class EntryPoint
{
	public static void main( String... args ) throws Exception
	{
		// Prepare the environment by downloading and applying the builtin libraries required
		Foundation.prepare();

		// Specify the BaseApplication for this environment.
		MinimalApplication app = new MinimalApplication();
		Foundation.setApplication( app );

		app.addArgument( "start", "Starts the daemon" );
		app.addArgument( "stop", "Stops the daemon" );
		app.addArgument( "status", "Prints the active daemon list" );

		final String instanceId = app.getEnv().getString( "instance-id" ).orElse( null );

		// Load up Network UDP Driver
		final UDPWorker udp = NetworkLoader.UDP();

		Events.getInstance().listen( app, RunlevelEvent.class, ( event ) -> {
			if ( event.getRunLevel() == Runlevel.MAINLOOP )
			{
				try
				{
					udp.start();
				}
				catch ( ApplicationException.Error e )
				{
					throw new StartupException( e );
				}

				if ( !udp.isStarted() )
					throw new StartupException( "The UDP service failed to start for unknown reasons." );
			}
			if ( event.getRunLevel() == Runlevel.STARTED )
			{
				if ( app.hasArgument( "status" ) )
				{
					Foundation.L.info( "Waiting..." );
					try
					{
						IPC.status();
					}
					catch ( PacketValidationException packetValidation )
					{
						packetValidation.printStackTrace();
					}
				}
				else if ( app.hasArgument( "stop" ) )
				{
					Foundation.L.info( "Stopping..." );
					IPC.stop( instanceId );
				}
				else if ( app.hasArgument( "start" ) )
				{
					Foundation.L.info( "Starting..." );
					try
					{
						IPC.start();
					}
					catch ( PacketValidationException packetValidation )
					{
						packetValidation.printStackTrace();
					}
				}

			}
		} );

		try
		{
			app.parse( args );
		}
		catch ( StartupInterruptException e )
		{
			// Prevent exception from being printed to console
			return;
		}

		Foundation.start();
	}
}
