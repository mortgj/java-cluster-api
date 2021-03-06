package system;


import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;

import api.*;

public class ComputerImpl extends UnicastRemoteObject implements Computer  {

    private Space space;
    private Shared shared;
    private Task cached;
	public ComputerImpl(Space space) throws RemoteException {
		super();
        this.space = space;
        cached = null;

		// TODO Auto-generated constructor stub
	}

	@Override
	public Result execute(Task task) throws RemoteException {
        task.setComputer(this);
        Result result = task.execute();
        // TODO This part can be more elegant
        if (result instanceof ContinuationResult) {
            ContinuationTask continuationTask = (ContinuationTask) result.getTaskReturnValue();
            ArrayList<Task> tasks = continuationTask.getTasks();
            cached = tasks.get(0);
            cached.setCached(true);
            System.out.println("Cached a task for execution");
        }
        return result;
	}

    @Override
    public boolean hasCached() {
        return cached != null;
    }

    @Override
    public  Result executeCachedTask() throws RemoteException {
        System.out.println("Running a cached task");
        // hate too return _null_. Fix later
        if (cached == null) return null;
        Task task = cached;
        cached = null;
        return execute(task);
    }

	@Override
	public void stop() throws RemoteException {
		System.exit(0);		
	}

	public static void main(String[] args) {
		try {

            if (args.length == 0) {
                System.out.println("Argument missing");
                System.exit(0);
            }
            String url = args[0];
            int port = 8888;
            if (args.length == 2) {
                port = Integer.parseInt(args[1]);
            }


			String urlString = "rmi://"+url+":"+port+"/"+Space.SERVICE_NAME;
			System.out.println("Connecting to " + url + ":" + port + ". ");
			//Registry registry = LocateRegistry.getRegistry(url,port);
			final Space space = (Space) Naming.lookup(urlString);

            // en hack for å starte Computer i en tråd.
            for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ComputerImpl computer = null;
                        try {
                            computer = new ComputerImpl(space);
                            space.register(computer);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            System.exit(1);
                        }

                    }
                });
                t.start();
                System.out.println("Computer #"+(i + 1)+ " started.");
            }

			System.out.println("Computer successfully registered!");
		} catch (Exception e) {
			System.out.println("Something went wrong!");
			e.printStackTrace();
		}



	}

	@Override
	public synchronized Object getShared() throws RemoteException {
		return shared;
	}

    private synchronized boolean checkAndSetSharedThreadSafe(Shared shared) throws RemoteException {
        if (shared.isNewerThan(this.shared)) {
            this.shared = shared;
            return true;
        }
        return false;
    }

	@Override
	public  void setShared(Shared proposedShared) throws RemoteException {
		if (checkAndSetSharedThreadSafe(proposedShared))	{
		    space.setShared( shared );
		}
		
	}

    @Override
    public Space getSpace() throws RemoteException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setSpace(Space space) throws RemoteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
