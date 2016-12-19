#!/bin/bash
mysql -u root -e 'create database drupal;'
mysql -u root -e "GRANT ALL PRIVILEGES ON drupal.* To 'drupal'@'localhost' IDENTIFIED BY 'drupal';"

cd $HOME
pear channel-discover pear.drush.org
pear upgrade --force Console_GetoptPlus
pear upgrade --force pear
pear channel-discover pear.drush.org

# Drush
cd /tmp
php -r "readfile('https://s3.amazonaws.com/files.drush.org/drush.phar');" > drush
php drush core-status
chmod +x drush
sudo mv drush /opt
sudo ln -s /opt/drush /usr/bin/drush

phpenv rehash

cd $HOME
drush dl drupal-8.2.2 --drupal-project-rename=drupal
cd $HOME/drupal
drush si minimal --db-url=mysql://drupal:drupal@localhost/drupal --yes
drush runserver --php-cgi=$HOME/.phpenv/shims/php-cgi localhost:8081 &>/tmp/drush_webserver.log &

ln -s $ISLANDORA_DIR modules/islandora
drush en -y rdf
drush en -y responsive_image
drush en -y syslog
drush en -y serialization
drush en -y basic_auth
drush en -y rest
drush en -y simpletest

drush dl rdfui --dev
drush en -y rdfui
drush en -y rdf_builder

drush dl restui
drush en -y restui

drush dl inline_entity_form
drush en -y inline_entity_form

drush dl media_entity
drush en -y media_entity

drush dl media_entity_image
drush en -y media_entity_image

drush dl search_api
drush -y pm-uninstall search
drush en -y search_api

drush dl typed_data
drush en -y typed_data 

drush dl rules
drush en -y rules 

cd $HOME/drupal/modules
git clone https://github.com/DiegoPino/claw-jsonld.git
drush en -y jsonld

drush en -y islandora

drush -y dl bootstrap
drush -y en bootstrap
drush -y config-set system.theme default bootstrap

drush cr
# The shebang in this file is a bogeyman that is haunting the web test cases.
rm /home/travis/.phpenv/rbenv.d/exec/hhvm-switcher.bash
sleep 20
