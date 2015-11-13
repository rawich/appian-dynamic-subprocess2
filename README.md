# Dynamic Sub-Process 2 - Smart Service Plug-in for Appian
Appian Smart Service plug-in, for non-cloud (on premise) environment, to dynamically start asynchronous sub-process using process model ID or UUID.

> Derived from Dynamic Subprocess (Async), originally created by Ryan Gates (Appian Corp / April 2013) / Updated by [Rawich Poomrin](https://www.linkedin.com/in/rawich) (November 2015)

Appian Smart Service plug-in to dynamically start asynchronous sub-process using process model id or UUID, which can be determined at run-time.

This is a derivative of and extension to Dynamic Subprocess (Async) shared component by Ryan Gates in Appian Forum.
 
## Dynamic Sub-Process II (Smart Service node)
 - This is a new Smart Service created to address the need to specify a different user as process initiator, while using administrator context to execute the Smart Service.  
 - This Smart Service node can be configured to "Run as whoever designed this process model" to make sure the user context will have permission to lookup process model by UUID and interrogate the model for process variable information and still possible to specify a basic user to be the initiator of the new process.
 
 - There are two main error scenarios, with appropriate error messages from resource bundle: 
 - 1) Looking up of process model ID from UUID failed (permission issue or process model with the specified UUID does not exist)
 - 2) Starting the subprocess failed (most likely because the user who executes this node does not have enough security access to the target process model.
 
 Key differentiations from the original Dynamic Subprocess (Async): 
 - This plug-in addresses an issue of Exception swallowing. In case of error, and no sub-process is started, the original plug-in swallows Exception and caused the node to look like completed successfully (from Appian Designer portal and Monitor Process).
 - If there is any error condition, the error will be reported both in the log file, and Alert, and the node will be paused by exception. 
 - The node will still fail to start the intended sub-process if UUID is used to identify the sub-process and the Run-As user is not a system administrator. The workaround is to use new input "Sub-Process Initiator" to specify initiator of the sub-process, but use system administrator to execute the node.

## Contribute

Contributions are always welcome!
You'll need to use "Request Permission to Edit a Component" action in Appian Forum to gain access to update this shared component.


## License

[The MIT License](https://github.com/rawich/appian-dynamic-subprocess2/blob/master/LICENSE)

To the extent possible under law, [Rawich Poomrin](https://www.linkedin.com/in/rawich) has waived all copyright and related or neighboring rights to this work.