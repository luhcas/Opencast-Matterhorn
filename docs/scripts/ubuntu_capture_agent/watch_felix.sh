#! /bin/bash

######################
# Felix Watch Script #
######################

# MH-4493: This script will periodically check to see if a capture agent
# is running and, if not, starts the capture agent and send an email

# Grab all of the necessary properties from the configuration
CONFIGURATION="$FELIX_HOME/conf/services/org.opencastproject.capture.impl.ConfigurationManager.properties"
ERROR_EMAILS=`sed '/^\#/d' $CONFIGURATION | grep 'capture.error.emails'  | tail -n 1 | sed 's/^.*=//'`
ERROR_SMTP=`sed '/^\#/d' $CONFIGURATION | grep 'capture.error.smtp'  | tail -n 1 | sed 's/^.*=//'`
ERROR_SMTP_USER=`sed '/^\#/d' $CONFIGURATION | grep 'capture.error.smtp.user'  | tail -n 1 | sed 's/^.*=//'`
ERROR_SMTP_PASSWD=`sed '/^\#/d' $CONFIGURATION | grep 'capture.error.smtp.password'  | tail -n 1 | sed 's/^.*=//'`
ERROR_SUBJECT=`sed '/^\#/d' $CONFIGURATION | grep 'capture.error.subject'  | tail -n 1 | sed 's/^.*=//'`
ERROR_MSGBODY=`sed '/^\#/d' $CONFIGURATION | grep 'capture.error.messagebody'  | tail -n 1 | sed 's/^.*=//'`


# Determine is the Felix process is running
PROCESS=`ps aux | grep java | grep felix.jar`
if [ -z "$PROCESS" ]; then  
	$FELIX_HOME/bin/start_matterhorn.sh &> /dev/null &
	DATE=`date`
	SUBJECT=`echo $ERROR_SUBJECT | sed "s/%date/$DATE/g" | sed "s/%hostname/$HOSTNAME/g" | sed "s/\"//g"`
	MESSAGE=`echo $ERROR_MSGBODY | sed "s/%date/$DATE/g" | sed "s/%hostname/$HOSTNAME/g" | sed "s/\"//g"`
	echo $MESSAGE | mail -s "$SUBJECT" -t $ERROR_EMAILS
	echo "$MESSAGE" > /dev/stderr
else
	echo "Felix is running."
fi

