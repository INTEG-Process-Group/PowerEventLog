# PowerEventLog
The application monitors the JNIOR runtime and downtime and writes it to a powerevents.log file.  This application helps detect power loss.  It can also help audit missed events by knwing that the reason they were missed was due to a power outage.

# How it Works
The application starts up and checks the immutable block to see if there is any uptime and last known runtime information.  If the data is present then a log entry is created.  The application then goes into a loop forever and once a second the timestamp representing the last known uptime is recorded.
