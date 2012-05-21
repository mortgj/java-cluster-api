package system;

import api.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class ComputerProxy extends UnicastRemoteObject implements Runnable, Computer {

    private Computer computer;
    protected Space space;
    private Task cached;

    /**
     * Creates a Proxy for handling Computers
     * @param computer The computer you wish to proxy
     * @param space On what space the tasks and results are published
     */
    public ComputerProxy(Computer computer, Space space) throws RemoteException {
        super();
        this.computer = computer;
        this.space = space;
        this.cached = null;
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public Result execute(Task task) throws RemoteException {
        return computer.execute(task);
    }

    @Override
    public void stop() throws RemoteException {
        computer.stop();
    }

	@Override
	public Object getShared() throws RemoteException {
		return computer.getShared();
	}

	@Override
	public void setShared(Shared shared) throws RemoteException {
		computer.setShared(shared);
		
	}

    @Override
    public boolean hasCached() throws RemoteException {
        return computer.hasCached();
    }

    @Override
    public Result executeCachedTask() throws RemoteException {
        return computer.executeCachedTask();
    }


    private void handleFaultyComputer(Task task) throws InterruptedException {
        try {
            space.put(task);
            deregisterComputer();
        } catch (RemoteException ignore) { }
    }

    private void deregisterComputer() {
        try {
            space.deregister(this);
        } catch (RemoteException ignore) { }
    }

    private void putResultToSpace(Result result) {
        try {
            space.putResult(result);
        }
        catch (RemoteException ignore) {}
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start the ComputerProxy process
     * It waits on a client to publish a task. When a task is published
     * it tires to assign the task to its corresponding Computer.
     * If the computer returns a result it sends the result back to the space.
     * However, if the Computer raises a RemoteException this proxy puts the
     * task back into the space, and deregisters it self.
     */
    public void run() {
        // TODO Clean!
        // I think this method has become too complex. Is there a way we can simply it?
        System.out.println("ComputerProxy running");
        do {

                Result result = null;
                // if computer has a cached task execute that one. If not get one from space
            try {
                if (hasCached() && cached != null) {
                    try {
                        result = computer.executeCachedTask();
                        if (result == null) continue;   // the cached task has already been executed. This should never happen since
                                                        // Computer should be single threaded (Intended design)
                    } catch (RemoteException e) {
                        System.out.println("A computer has crashed. Putting the cached task back to space");
                        handleFaultyComputer(cached);
                        return;
                    }
                    cached = null;
                }
                else {
                    Task task = space.takeTask();
                    try {
                        result = execute(task);
                    } catch (RemoteException e) {
                            System.out.println("A computer has crashed. Putting the currently running task back to space");
                            handleFaultyComputer(task);
                            return;          // exit thread . The proxy is no longer needed
                    }
                }

                if (result instanceof ContinuationResult) {
                    // One should not be able to access the ContinuationTask in this way. Its error-prone.
                    // TODO find a better way to retrieve tasks marked as cached
                    ContinuationTask continuationTask = (ContinuationTask) result.getTaskReturnValue();
                    ArrayList<Task> tasks = continuationTask.getTasks();
                    Task task;
                    if ((task = tasks.get(0)).getCached() && this.cached == null) {
                        System.out.println("A Computer has cached a task");
                        this.cached = task;
                    }
                }
            } catch (RemoteException e) {
                System.out.println("A computer crashed before a task was scheduled.");
                deregisterComputer();
                return;
            }
            catch (InterruptedException e) {
                e.printStackTrace();            // don't know how we shall handle this one, yet...
            }
            putResultToSpace(result);
        } while(true);
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
