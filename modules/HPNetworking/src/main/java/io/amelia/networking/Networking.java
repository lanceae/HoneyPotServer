package io.amelia.networking;

import io.amelia.foundation.Kernel;
import io.amelia.foundation.binding.Facades;

public class Networking
{
	public static final Kernel.Logger L = Kernel.getLogger( Networking.class );

	public static NetworkingService i()
	{
		return Facades.getFacade( NetworkingService.class );
	}

	public static class HTTP
	{
		private HTTP()
		{
			// Static Access
		}


	}

	public static class TCP
	{
		private TCP()
		{
			// Static Access
		}


	}

	public static class UDP
	{
		private UDP()
		{
			// Static Access
		}


	}
}
