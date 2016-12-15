#!/bin/bash

# this script requires that your client repo be located in ~/git/clients
# also assumes that you have symlinked host.yml
#  ln -sfn ~/git/clients/hosts.yml hosts.yml

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "*** starting script run ***"
date

# sync clients repo
cd $HOME/git/clients/
echo "pulling most recent commit of clients repo"
git pull

# run kbloader, first cd to the directory with this sync script
cd $SCRIPTDIR && pwd
echo "running keybox loader script"

# if running on keybox, do special stuffs
if [[ "$HOSTNAME" = keybox* ]]; then
echo "sourcing rvm manually"
source /home/lookerops/.rvm/environments/jruby-1.7.19
fi

bundle install
bundle exec ./kbloader.rb

# complete!
echo "*** script complete ***"