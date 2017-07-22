package io.amelia.networking.packets;

@SuppressWarnings( "unchecked" )
public abstract class PacketRequest<T> extends Packet<T>
{
	// 15 Second Response Timeout
	private long timeout = 15;

	public T setTimeout( long timeout )
	{
		if ( timeout > 300 )
			throw new IllegalArgumentException( "Timeout has a max timeout of 5 minutes! (300 seconds)" );
		this.timeout = timeout;
		return ( T ) this;
	}

	public long getTimeout()
	{
		return timeout;
	}
}
